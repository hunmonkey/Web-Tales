package org.tales.infrastructure.jooq.main

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.tales.domain.model.AgeRestriction
import org.tales.domain.model.AllowExclude
import org.tales.domain.model.ContentRestrictions
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.model.makeLibrary
import org.tales.domain.persistence.LibraryRepository
import org.tales.infrastructure.jooq.offset
import java.time.LocalDateTime

@SpringBootTest
class TalesUserDaoTest(
  @Autowired private val talesUserDao: TalesUserDao,
  @Autowired private val libraryRepository: LibraryRepository,
) {
  private val library = makeLibrary()

  @BeforeAll
  fun setup() {
    libraryRepository.insert(library)
  }

  @AfterEach
  fun deleteUsers() {
    talesUserDao.deleteAll()
    assertThat(talesUserDao.count()).isEqualTo(0)
  }

  @AfterAll
  fun tearDown() {
    libraryRepository.deleteAll()
  }

  @Test
  fun `given a user when saving it then it is persisted`() {
    val now = LocalDateTime.now()
    val user =
      TalesUser(
        email = "user@example.org",
        password = "password",
        sharedLibrariesIds = setOf(library.id),
        sharedAllLibraries = false,
      )

    talesUserDao.insert(user)
    val created = talesUserDao.findByIdOrNull(user.id)!!

    with(created) {
      assertThat(id).isNotEqualTo(0)
      assertThat(createdDate).isCloseTo(now, offset)
      assertThat(lastModifiedDate).isCloseTo(now, offset)
      assertThat(email).isEqualTo("user@example.org")
      assertThat(password).isEqualTo("password")
      assertThat(roles).containsExactlyInAnyOrder(UserRoles.FILE_DOWNLOAD, UserRoles.PAGE_STREAMING)
      assertThat(sharedLibrariesIds).containsExactly(library.id)
      assertThat(sharedAllLibraries).isFalse
      assertThat(restrictions.ageRestriction).isNull()
      assertThat(restrictions.labelsAllow).isEmpty()
      assertThat(restrictions.labelsExclude).isEmpty()
    }
  }

  @Test
  fun `given existing user when modifying and saving it then it is persisted`() {
    val user =
      TalesUser(
        email = "user@example.org",
        password = "password",
        sharedLibrariesIds = setOf(library.id),
        sharedAllLibraries = false,
        restrictions =
          ContentRestrictions(
            ageRestriction = AgeRestriction(10, AllowExclude.ALLOW_ONLY),
            labelsAllow = setOf("allow"),
            labelsExclude = setOf("exclude"),
          ),
      )

    talesUserDao.insert(user)
    val created = talesUserDao.findByIdOrNull(user.id)!!
    with(created) {
      assertThat(restrictions.ageRestriction).isNotNull
      assertThat(restrictions.ageRestriction!!.age).isEqualTo(10)
      assertThat(restrictions.ageRestriction!!.restriction).isEqualTo(AllowExclude.ALLOW_ONLY)
      assertThat(restrictions.labelsAllow).containsExactly("allow")
      assertThat(restrictions.labelsExclude).containsExactly("exclude")
    }

    val modified =
      created.copy(
        email = "user2@example.org",
        password = "password2",
        roles = setOf(UserRoles.ADMIN, UserRoles.FILE_DOWNLOAD, UserRoles.PAGE_STREAMING),
        sharedLibrariesIds = emptySet(),
        sharedAllLibraries = true,
        restrictions =
          ContentRestrictions(
            ageRestriction = AgeRestriction(16, AllowExclude.EXCLUDE),
            labelsAllow = setOf("allow2"),
            labelsExclude = setOf("exclude2"),
          ),
      )
    val modifiedDate = LocalDateTime.now()
    talesUserDao.update(modified)
    val modifiedSaved = talesUserDao.findByIdOrNull(modified.id)!!

    with(modifiedSaved) {
      assertThat(id).isEqualTo(created.id)
      assertThat(createdDate).isEqualTo(created.createdDate)
      assertThat(lastModifiedDate)
        .isCloseTo(modifiedDate, offset)
        .isNotEqualTo(modified.createdDate)
      assertThat(email).isEqualTo("user2@example.org")
      assertThat(password).isEqualTo("password2")
      assertThat(roles).containsExactlyInAnyOrder(UserRoles.ADMIN, UserRoles.FILE_DOWNLOAD, UserRoles.PAGE_STREAMING)
      assertThat(sharedLibrariesIds).isEmpty()
      assertThat(sharedAllLibraries).isTrue
      assertThat(restrictions.ageRestriction).isNotNull
      assertThat(restrictions.ageRestriction!!.age).isEqualTo(16)
      assertThat(restrictions.ageRestriction!!.restriction).isEqualTo(AllowExclude.EXCLUDE)
      assertThat(restrictions.labelsAllow).containsExactly("allow2")
      assertThat(restrictions.labelsExclude).containsExactly("exclude2")
    }

    talesUserDao.update(modifiedSaved.copy(restrictions = ContentRestrictions()))
    with(talesUserDao.findByIdOrNull(modified.id)!!) {
      assertThat(restrictions.ageRestriction).isNull()
      assertThat(restrictions.labelsAllow).isEmpty()
      assertThat(restrictions.labelsExclude).isEmpty()
    }
  }

  @Test
  fun `given multiple users when saving then they are persisted`() {
    talesUserDao.insert(TalesUser("user1@example.org", "p"))
    talesUserDao.insert(TalesUser("user2@example.org", "p"))

    val users = talesUserDao.findAll()

    assertThat(users).hasSize(2)
    assertThat(users.map { it.email }).containsExactlyInAnyOrder(
      "user1@example.org",
      "user2@example.org",
    )
  }

  @Test
  fun `given some users when counting then proper count is returned`() {
    talesUserDao.insert(TalesUser("user1@example.org", "p"))
    talesUserDao.insert(TalesUser("user2@example.org", "p"))

    val count = talesUserDao.count()

    assertThat(count).isEqualTo(2)
  }

  @Test
  fun `given existing user when finding by id then user is returned`() {
    val existing = TalesUser("user1@example.org", "p")
    talesUserDao.insert(existing)

    val user = talesUserDao.findByIdOrNull(existing.id)

    assertThat(user).isNotNull
  }

  @Test
  fun `given non-existent user when finding by id then null is returned`() {
    val user = talesUserDao.findByIdOrNull("38473")

    assertThat(user).isNull()
  }

  @Test
  fun `given existing user when deleting then user is deleted`() {
    val existing = TalesUser("user1@example.org", "p")
    talesUserDao.insert(existing)

    talesUserDao.delete(existing.id)

    assertThat(talesUserDao.count()).isEqualTo(0)
  }

  @Test
  fun `given users when checking if exists by email then return true or false`() {
    talesUserDao.insert(
      TalesUser("user1@example.org", "p"),
    )

    val exists = talesUserDao.existsByEmailIgnoreCase("USER1@EXAMPLE.ORG")
    val notExists = talesUserDao.existsByEmailIgnoreCase("USER2@EXAMPLE.ORG")

    assertThat(exists).isTrue()
    assertThat(notExists).isFalse()
  }

  @Test
  fun `given users when finding by email then return user`() {
    talesUserDao.insert(
      TalesUser("user1@example.org", "p"),
    )

    val found = talesUserDao.findByEmailIgnoreCaseOrNull("USER1@EXAMPLE.ORG")
    val notFound = talesUserDao.findByEmailIgnoreCaseOrNull("USER2@EXAMPLE.ORG")

    assertThat(found).isNotNull
    assertThat(found?.email).isEqualTo("user1@example.org")
    assertThat(notFound).isNull()
  }

  @Test
  fun `given user when saving announcement as read then it works`() {
    val user = TalesUser("user1@example.org", "p")
    talesUserDao.insert(user)

    assertThat(talesUserDao.findAnnouncementIdsReadByUserId(user.id)).isEmpty()

    talesUserDao.saveAnnouncementIdsRead(user, setOf("1"))
    assertThat(talesUserDao.findAnnouncementIdsReadByUserId(user.id)).containsExactlyInAnyOrder("1")

    talesUserDao.saveAnnouncementIdsRead(user, setOf("2"))
    assertThat(talesUserDao.findAnnouncementIdsReadByUserId(user.id)).containsExactlyInAnyOrder("1", "2")

    talesUserDao.saveAnnouncementIdsRead(user, setOf("2"))
    assertThat(talesUserDao.findAnnouncementIdsReadByUserId(user.id)).containsExactlyInAnyOrder("1", "2")
  }
}
