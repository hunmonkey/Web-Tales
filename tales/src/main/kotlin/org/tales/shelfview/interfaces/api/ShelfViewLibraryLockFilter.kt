package org.tales.shelfview.interfaces.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.tales.infrastructure.security.TalesPrincipal

/**
 * Serves every content request as a user whose authorized libraries exclude
 * the PIN-locked ones this session has not unlocked (ADR-0022). Swapping the
 * principal here, after Spring Security has authenticated, puts one choke
 * point in front of the 80+ places that filter queries through the user's
 * authorized library ids - REST, OPDS and page streaming alike. Only the
 * request thread's context is replaced; the session's stored authentication
 * is left untouched, so an unlock applies from the very next request.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class ShelfViewLibraryLockFilter(
  private val lockService: ShelfViewLibraryLockService,
) : OncePerRequestFilter() {
  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    val path = request.requestURI
    return !(path.startsWith("/api") || path.startsWith("/opds") || path.startsWith("/sse"))
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val originalContext = SecurityContextHolder.getContext()
    val authentication = originalContext.authentication
    val principal = authentication?.principal as? TalesPrincipal
    if (principal == null) {
      filterChain.doFilter(request, response)
      return
    }

    val effectiveUser = lockService.effectiveUser(principal.user, request.getSession(false))
    if (effectiveUser === principal.user) {
      filterChain.doFilter(request, response)
      return
    }

    val effectivePrincipal =
      TalesPrincipal(
        user = effectiveUser,
        oAuth2User = principal.oAuth2User,
        oidcUser = principal.oidcUser,
        apiKey = principal.apiKey,
        name = principal.username,
      )
    val lockedContext = SecurityContextHolder.createEmptyContext()
    lockedContext.authentication = PrincipalOverrideAuthentication(authentication, effectivePrincipal)

    try {
      SecurityContextHolder.setContext(lockedContext)
      filterChain.doFilter(request, response)
    } finally {
      SecurityContextHolder.setContext(originalContext)
    }
  }

  /** The original authentication (type, authorities, flags) with only the principal replaced. */
  private class PrincipalOverrideAuthentication(
    private val delegate: Authentication,
    private val principal: TalesPrincipal,
  ) : Authentication by delegate {
    override fun getPrincipal(): Any = principal
  }
}
