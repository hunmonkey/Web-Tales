package org.tales.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.tales.Application

@AnalyzeClasses(packagesOf = [Application::class], importOptions = [ImportOption.DoNotIncludeTests::class])
class SlicesIsolationRulesTest {
  @ArchTest
  val interfacesShouldOnlyUseTheirOwnSlice: ArchRule =
    slices()
      .matching("..interfaces.(*)..")
      .namingSlices("Interface $1")
      .`as`("Interfaces")
      .should()
      .notDependOnEachOther()
}
