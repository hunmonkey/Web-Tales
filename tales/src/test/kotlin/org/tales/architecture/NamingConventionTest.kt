package org.tales.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.tales.Application

@AnalyzeClasses(packagesOf = [Application::class], importOptions = [ImportOption.DoNotIncludeTests::class])
class NamingConventionTest {
  @ArchTest
  val servicesShouldNotHaveNamesContainingServiceOrManager: ArchRule =
    noClasses()
      .that()
      .resideInAnyPackage("..domain..service..", "..application..service..")
      .should()
      .haveSimpleNameContaining("service")
      .orShould()
      .haveSimpleNameContaining("Service")
      .orShould()
      .haveSimpleNameContaining("manager")
      .orShould()
      .haveSimpleNameContaining("Manager")
      .because("it doesn't bear any intent")

  @ArchTest
  val controllersShouldBeSuffixed: ArchRule =
    classes()
      .that()
      .areAnnotatedWith(RestController::class.java)
      .or()
      .areAnnotatedWith(Controller::class.java)
      .should()
      .haveSimpleNameEndingWith("Controller")
}
