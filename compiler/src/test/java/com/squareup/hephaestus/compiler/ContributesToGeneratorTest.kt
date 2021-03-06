package com.squareup.hephaestus.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.Result
import org.junit.Test

@ExperimentalStdlibApi
class ContributesToGeneratorTest {

  @Test fun `there is no hint for merge annotations`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.MergeComponent
        
        @MergeComponent(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface.hint).isNull()
    }

    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.MergeSubcomponent
        
        @MergeSubcomponent(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface.hint).isNull()
    }
  }

  @Test fun `there is a hint for contributed Dagger modules`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.hint?.java).isEqualTo(daggerModule1)
    }
  }

  @Test fun `there is a hint for contributed interfaces`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        interface ContributingInterface
    """
    ) {
      assertThat(contributingInterface.hint?.java).isEqualTo(contributingInterface)
    }
  }

  @Test fun `there is a hint for contributed inner Dagger modules`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        interface ComponentInterface {
          @ContributesTo(Any::class)
          @dagger.Module
          abstract class InnerModule
        }
    """
    ) {
      assertThat(innerModule.hint?.java).isEqualTo(innerModule)
    }
  }

  @Test fun `there is a hint for contributed inner interfaces`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        class SomeClass {
          @ContributesTo(Any::class)
          interface InnerInterface
        }
    """
    ) {
      assertThat(innerInterface.hint?.java).isEqualTo(innerInterface)
    }
  }

  @Test fun `contributing module must be a Dagger Module`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        abstract class DaggerModule1
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (6, 16)")
    }
  }

  @Test fun `contributed modules must be public`() {
    val visibilities = setOf(
        "internal", "private", "protected"
    )

    visibilities.forEach { visibility ->
      compile(
          """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        $visibility abstract class DaggerModule1
    """
      ) {
        assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
        // Position to the class.
        assertThat(messages).contains("Source.kt: (7, ")
      }
    }
  }

  @Test fun `contributed interfaces must be public`() {
    val visibilities = setOf(
        "internal", "private", "protected"
    )

    visibilities.forEach { visibility ->
      compile(
          """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        $visibility interface ContributingInterface
    """
      ) {
        assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
        // Position to the class.
        assertThat(messages).contains("Source.kt: (6, ")
      }
    }
  }

  private fun compile(
    source: String,
    block: Result.() -> Unit = { }
  ): Result = compile(source, skipAnalysis = false, block = block)
}
