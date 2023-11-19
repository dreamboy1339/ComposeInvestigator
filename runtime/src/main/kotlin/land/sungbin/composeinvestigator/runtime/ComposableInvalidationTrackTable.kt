/*
 * Designed and developed by Ji Sungbin 2023.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/ComposeInvestigator/blob/main/LICENSE
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package land.sungbin.composeinvestigator.runtime

import org.jetbrains.annotations.VisibleForTesting

@ComposeInvestigatorCompilerApi
public data class DiffParams(
  public val name: String,
  public val params: List<Pair<ParameterInfo, ParameterInfo>>,
) {
  override fun toString(): String =
    buildString(capacity = params.size + 2) {
      appendLine("<$name> DiffParams(")
      if (params.isNotEmpty()) {
        for ((index, diffParam) in params.withIndex()) {
          val (prevParam, newParam) = diffParam
          val message =
            "${index + 1}. [${prevParam.name} <${prevParam.stability}>] " +
              "${prevParam.value} (${prevParam.hashCode}) -> ${newParam.value} (${newParam.hashCode})"
          appendLine("  $message")
        }
      } else {
        appendLine("  No diff params.")
        appendLine("  Some argument may be unstable, or there may have been an invalidation request on the current RecomposeScope.")
      }
      appendLine(")")
    }
}

@ComposeInvestigatorCompilerApi
public data class ParameterInfo(
  public val name: String,
  public val stability: DeclarationStability,
  public val value: String,
  public val hashCode: Int,
)

@ComposeInvestigatorCompilerApi
public class ComposableInvalidationTrackTable {
  @VisibleForTesting
  internal val parameterMap = mutableMapOf<String, Array<ParameterInfo>>()

  @ComposeInvestigatorCompilerApi
  public fun computeDiffParamsIfPresent(
    composableKeyName: String,
    composableOriginalName: String,
    vararg parameterInfos: ParameterInfo,
  ): DiffParams? {
    val prevParams = parameterMap[composableKeyName]

    if (prevParams == null) {
      @Suppress("UNCHECKED_CAST")
      parameterMap[composableKeyName] = parameterInfos as Array<ParameterInfo>
      return null
    }

    val diffs = mutableListOf<Pair<ParameterInfo, ParameterInfo>>()
    for ((index, prevParam) in prevParams.withIndex()) {
      if (prevParam.hashCode != parameterInfos[index].hashCode) {
        diffs.add(prevParam to parameterInfos[index])
      }
    }

    @Suppress("UNCHECKED_CAST")
    parameterMap[composableKeyName] = parameterInfos as Array<ParameterInfo>

    return DiffParams(name = composableOriginalName, params = diffs)
  }
}
