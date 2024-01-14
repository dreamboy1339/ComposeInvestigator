// COPIED FROM https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime/src/nonEmulatorCommonTest/kotlin/androidx/compose/runtime/mock/CompositionTest.kt;drc=a377689189fb94f207194bb6cbd384366fe1d92e.

/*
 * Designed and developed by Ji Sungbin 2023.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/ComposeInvestigator/blob/main/LICENSE
 */

@file:OptIn(InternalComposeApi::class, ExperimentalCoroutinesApi::class)
@file:Suppress("unused")

package land.sungbin.composeinvestigator.runtime.mock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

fun compositionTest(block: suspend CompositionTestScope.() -> Unit) = runTest {
  withContext(TestMonotonicFrameClock(this)) {
    // Start the recomposer
    val recomposer = Recomposer(coroutineContext)
    launch { recomposer.runRecomposeAndApplyChanges() }
    testScheduler.runCurrent()

    // Create a test scope for the test using the test scope passed in by runTest
    val scope = object : CompositionTestScope, CoroutineScope by this@runTest {
      var composed = false
      override var composition: Composition? = null

      override lateinit var root: View

      override val testCoroutineScheduler: TestCoroutineScheduler
        get() = this@runTest.testScheduler

      override fun compose(block: @Composable () -> Unit) {
        check(!composed) { "Compose should only be called once" }
        composed = true
        root = View().apply { name = "root" }
        val composition = Composition(ViewApplier(root), recomposer)
        this.composition = composition
        composition.setContent(block)
      }

      override fun advanceCount(ignorePendingWork: Boolean): Long {
        val changeCount = recomposer.changeCount
        Snapshot.sendApplyNotifications()
        if (recomposer.hasPendingWork) {
          advanceTimeBy(5_000)
          check(ignorePendingWork || !recomposer.hasPendingWork) {
            "Potentially infinite recomposition, still recomposing after advancing"
          }
        }
        return recomposer.changeCount - changeCount
      }

      override fun advanceTimeBy(amount: Long) = testScheduler.advanceTimeBy(amount)

      override fun advance(ignorePendingWork: Boolean) = advanceCount(ignorePendingWork) != 0L

      override fun verifyConsistent() {
        (composition as? ControlledComposition)?.verifyConsistent()
      }

      override var validator: (MockViewValidator.() -> Unit)? = null
    }
    scope.block()
    scope.composition?.dispose()
    recomposer.cancel()
    recomposer.join()
  }
}

/**
 * A test scope used in tests that allows controlling and testing composition.
 */
interface CompositionTestScope : CoroutineScope {
  /**
   * A scheduler used by [CoroutineScope]
   */
  val testCoroutineScheduler: TestCoroutineScheduler

  /**
   * Compose a block using the mock view composer.
   */
  fun compose(block: @Composable () -> Unit)

  /**
   * Advance the state which executes any pending compositions, if any. Returns true if
   * advancing resulted in changes being applied.
   */
  fun advance(ignorePendingWork: Boolean = false): Boolean

  /**
   * Advance counting the number of time the recomposer ran.
   */
  fun advanceCount(ignorePendingWork: Boolean = false): Long

  /**
   * Advance the clock by [amount] ms
   */
  fun advanceTimeBy(amount: Long)

  /**
   * Verify the composition is well-formed.
   */
  fun verifyConsistent()

  /**
   * The root mock view of the mock views being composed.
   */
  val root: View

  /**
   * The last validator used.
   */
  var validator: (MockViewValidator.() -> Unit)?

  /**
   * Access to the composition created for the call to [compose]
   */
  val composition: Composition?
}

/**
 * Create a mock view validator and validate the view.
 */
fun CompositionTestScope.validate(block: MockViewValidator.() -> Unit) =
  MockViewListValidator(root.children).validate(block).also { validator = block }

/**
 * Revalidate using the last validator
 */
fun CompositionTestScope.revalidate() =
  validate(validator ?: error("validate was not called"))

/**
 * Advance and expect changes
 */
fun CompositionTestScope.expectChanges() {
  val changes = advance()
  assertTrue(changes, "Expected changes but none were found")
}

/**
 * Advance and expect no changes
 */
fun CompositionTestScope.expectNoChanges() {
  val changes = advance()
  assertFalse(changes, "Expected no changes but changes occurred")
}

fun use(@Suppress("UNUSED_PARAMETER") value: Any) { }
