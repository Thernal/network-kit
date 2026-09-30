package io.thernal.networkkit.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import io.thernal.networkkit.detektrules.collections.UnsafeCollectionIndexAccess
import io.thernal.networkkit.detektrules.packageboundary.LayerPackageBoundary
import io.thernal.networkkit.detektrules.packageboundary.LayerPackageRequired
import io.thernal.networkkit.detektrules.preview.PreviewMustBePrivate
import io.thernal.networkkit.detektrules.style.ExpressionBodyNotAllowed
import io.thernal.networkkit.detektrules.style.MultilineConstructorRequired

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("project")

    override fun instance() = RuleSet(
        ruleSetId,
        listOf(
            ::PreviewMustBePrivate,
            ::UnsafeCollectionIndexAccess,
            ::LayerPackageBoundary,
            ::LayerPackageRequired,
            ::ExpressionBodyNotAllowed,
            ::MultilineConstructorRequired,
        ),
    )
}
