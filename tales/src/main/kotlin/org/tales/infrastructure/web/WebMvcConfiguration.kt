package org.tales.infrastructure.web

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.WebContentInterceptor
import java.util.concurrent.TimeUnit

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {
  companion object {
    /**
     * Files served from the root of `classpath:public/`.
     * Any new file added to `tales-webui/public` must be listed here, otherwise it falls back to index.html.
     */
    val rootStaticResources =
      listOf(
        "/index.html",
        "/favicon.ico",
        "/favicon-16x16.png",
        "/favicon-32x32.png",
        "/mstile-144x144.png",
        "/apple-touch-icon.png",
        "/apple-touch-icon-180x180.png",
        "/android-chrome-192x192.png",
        "/android-chrome-512x512.png",
        "/tales-icon.svg",
        "/tales-icon-square.png",
        "/manifest.json",
        "/shelfview-service-worker.js",
      )

    /**
     * Endpoints returning an image the browser may reuse without asking first, served with
     * [cacheImageContent] instead of the API-wide [cachePrivate].
     *
     * This is the single place the image cache policy is decided. Setting `Cache-Control` on the
     * `ResponseEntity` in a handler would not work: response entity headers are added to the
     * servlet response rather than set on it, so both values would be sent and the one written
     * here would still be the one that counts.
     *
     * A poster is safe to keep because the web UI appends a cache-busting query to its URL when
     * the matching thumbnail SSE event arrives. A page image is safe because its content only
     * changes when the book file is replaced, which re-analyzes the media.
     *
     * Deliberately excluded, because the reader has to see their current version: the page list
     * (`/pages`, no trailing segment), `/positions`, manifests, and EPUB resources.
     */
    val imageResources =
      listOf(
        "/api/v1/books/*/thumbnail",
        "/api/v1/books/*/thumbnails/*",
        "/api/v1/series/*/thumbnail",
        "/api/v1/series/*/thumbnails/*",
        "/api/v1/collections/*/thumbnail",
        "/api/v1/collections/*/thumbnails/*",
        "/api/v1/readlists/*/thumbnail",
        "/api/v1/readlists/*/thumbnails/*",
        "/api/v1/books/*/pages/*",
        "/api/v1/books/*/pages/*/thumbnail",
        "/api/v1/books/*/pages/*/raw",
        "/api/shelfview/v1/books/*/image-directory/pages/*",
        "/api/shelfview/v1/books/*/mixed/*/pages/*",
      )
  }

  override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    if (!registry.hasMappingForPattern("/webjars/**")) {
      registry
        .addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/")
    }

    if (!registry.hasMappingForPattern("/swagger-ui.html**")) {
      registry
        .addResourceHandler("/swagger-ui.html**")
        .addResourceLocations("classpath:/META-INF/resources/")
    }

    registry
      .addResourceHandler(*rootStaticResources.toTypedArray())
      .addResourceLocations("classpath:public/")
      .setCacheControl(CacheControl.noStore())

    listOf("css", "fonts", "img", "js")
      .forEach {
        registry
          .addResourceHandler("/$it/**")
          .addResourceLocations("classpath:public/$it/")
          .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
      }
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(
      WebContentInterceptor().apply {
        addCacheMapping(
          cachePrivate,
          "/api/**",
          "/opds/**",
        )
      },
    )

    // Images override the API-wide policy above. This is a second interceptor rather than a
    // second mapping on the same one, because WebContentInterceptor holds its mappings in a
    // HashMap and returns the first pattern that matches, which is not deterministic once two
    // patterns overlap. Registered afterwards, so its Cache-Control replaces the one already set.
    registry
      .addInterceptor(
        WebContentInterceptor().apply {
          addCacheMapping(cacheImageContent, "/**")
        },
      ).addPathPatterns(imageResources)
  }

  // The interceptors above set Cache-Control before the handler runs, so an
  // error response would keep the day-long image policy and a passing 404 (a
  // NAS hiccup, a re-analysis) would stay broken in the browser for a day.
  // Runs ahead of the real resolvers and leaves the resolving to them.
  override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
    resolvers.add(0) { _, response, _, _ ->
      response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().headerValue)
      null
    }
  }

  override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
    resolvers.add(AuthorsHandlerMethodArgumentResolver())
    resolvers.add(DelimitedPairHandlerMethodArgumentResolver())
  }
}
