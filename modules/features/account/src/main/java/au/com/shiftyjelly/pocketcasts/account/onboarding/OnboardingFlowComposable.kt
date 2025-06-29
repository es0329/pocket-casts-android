package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeFlow
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingAccountBenefitsViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getSerializableCompat

@Composable
fun OnboardingFlowComposable(
    featuresViewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    if (flow is OnboardingFlow.PlusAccountUpgrade) {
        Content(
            featuresViewModel = featuresViewModel,
            state = state,
            theme = theme,
            flow = flow,
            exitOnboarding = exitOnboarding,
            completeOnboardingToDiscover = completeOnboardingToDiscover,
            signInState = signInState,
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )
    } else {
        AppThemeWithBackground(theme) {
            Content(
                featuresViewModel = featuresViewModel,
                state = state,
                theme = theme,
                flow = flow,
                exitOnboarding = exitOnboarding,
                completeOnboardingToDiscover = completeOnboardingToDiscover,
                signInState = signInState,
                navController = navController,
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }
    }
}

@Composable
private fun Content(
    featuresViewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState,
    navController: NavHostController,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val startDestination = when (flow) {
        is OnboardingFlow.LoggedOut,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        is OnboardingFlow.InitialOnboarding,
        is OnboardingFlow.EngageSdk,
        is OnboardingFlow.ReferralLoginOrSignUp,
        -> OnboardingNavRoute.logInOrSignUp

        // Cannot use OnboardingNavRoute.PlusUpgrade.routeWithSource here, it is set as a defaultValue in the PlusUpgrade composable,
        // see https://stackoverflow.com/a/70410872/1910286
        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PatronAccountUpgrade,
        is OnboardingFlow.Upsell,
        is OnboardingFlow.UpsellSuggestedFolder,
        -> OnboardingNavRoute.PlusUpgrade.route

        is OnboardingFlow.Welcome -> OnboardingNavRoute.welcome

        is OnboardingFlow.AccountEncouragement -> OnboardingNavRoute.encourageFreeAccount
    }

    val onAccountCreated: () -> Unit = {
        fun goBack() {
            navController.navigate(OnboardingRecommendationsFlow.route) {
                // clear backstack after account is created
                popUpTo(OnboardingNavRoute.logInOrSignUp) {
                    inclusive = true
                }
            }
        }

        fun goToUpsell() {
            navController.navigate(OnboardingNavRoute.PlusUpgrade.routeWithSource(flow.source, forcePurchase = true)) {
                // clear backstack after account is created
                popUpTo(OnboardingNavRoute.logInOrSignUp) {
                    inclusive = true
                }
            }
        }

        when (flow) {
            is OnboardingFlow.ReferralLoginOrSignUp -> {
                exitOnboarding(OnboardingExitInfo.ShowReferralWelcome)
            }

            is OnboardingFlow.Upsell, is OnboardingFlow.UpsellSuggestedFolder -> {
                if (flow.source in forcedPurchaseSources) {
                    goToUpsell()
                } else {
                    goBack()
                }
            }

            else -> {
                goBack()
            }
        }
    }

    fun finishOnboardingFlow() {
        val exitInfo = if (flow is OnboardingFlow.UpsellSuggestedFolder) {
            OnboardingExitInfo.ApplySuggestedFolders(flow.action)
        } else {
            OnboardingExitInfo.Simple
        }
        exitOnboarding(exitInfo)
    }

    NavHost(navController, startDestination) {
        importFlowGraph(theme, navController, flow, onUpdateSystemBars)

        onboardingRecommendationsFlowGraph(
            theme,
            flow = flow,
            onBackPressed = { exitOnboarding(OnboardingExitInfo.Simple) },
            onComplete = {
                navController.navigate(
                    if (signInState.isSignedInAsPlusOrPatron) {
                        OnboardingNavRoute.welcome
                    } else {
                        OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.RECOMMENDATIONS)
                    },
                )
            },
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )

        composable(OnboardingNavRoute.encourageFreeAccount) {
            val viewModel = hiltViewModel<OnboardingAccountBenefitsViewModel>()

            CallOnce {
                viewModel.onScreenShown()
            }

            AppTheme(theme) {
                AccountBenefitsPage(
                    onGetStarted = {
                        viewModel.onGetStartedClick()
                        navController.navigate(OnboardingNavRoute.logInOrSignUp) {
                            popUpTo(OnboardingNavRoute.encourageFreeAccount) {
                                inclusive = true
                            }
                        }
                    },
                    onLogIn = {
                        viewModel.onLogInClick()
                        navController.navigate(OnboardingNavRoute.logInOrSignUp) {
                            popUpTo(OnboardingNavRoute.encourageFreeAccount) {
                                inclusive = true
                            }
                        }
                    },
                    onClose = {
                        viewModel.onDismissClick()
                        exitOnboarding(OnboardingExitInfo.Simple)
                    },
                    onBenefitShown = { benefit ->
                        viewModel.onBenefitShown(benefit.analyticsValue)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(OnboardingNavRoute.logInOrSignUp) {
            OnboardingLoginOrSignUpPage(
                theme = theme,
                flow = flow,
                onDismiss = {
                    when (flow) {
                        // This should never happen. If the user isn't logged in they should be in the AccountUpgradeNeedsLogin flow
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.PatronAccountUpgrade,
                        is OnboardingFlow.Welcome,
                        -> error("Account upgrade flow tried to present LoginOrSignupPage")

                        is OnboardingFlow.AccountEncouragement,
                        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.Upsell,
                        is OnboardingFlow.UpsellSuggestedFolder,
                        -> {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                exitOnboarding(OnboardingExitInfo.Simple)
                            }
                        }

                        is OnboardingFlow.InitialOnboarding,
                        is OnboardingFlow.LoggedOut,
                        is OnboardingFlow.EngageSdk,
                        is OnboardingFlow.ReferralLoginOrSignUp,
                        -> exitOnboarding(OnboardingExitInfo.Simple)
                    }
                },
                onSignUpClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                onContinueWithGoogleComplete = { state, subscription ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        onLoginToExistingAccount(flow, subscription, exitOnboarding, navController)
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.createFreeAccount) {
            OnboardingCreateAccountPage(
                theme = theme,
                onBackPressed = { navController.popBackStack() },
                onAccountCreated = onAccountCreated,
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.logIn) {
            OnboardingLoginPage(
                theme = theme,
                onBackPressed = { navController.popBackStack() },
                onLoginComplete = { subscription ->
                    onLoginToExistingAccount(flow, subscription, exitOnboarding, navController)
                },
                onForgotPasswordTapped = { navController.navigate(OnboardingNavRoute.forgotPassword) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.forgotPassword) {
            OnboardingForgotPasswordPage(
                theme = theme,
                onBackPressed = { navController.popBackStack() },
                onCompleted = { exitOnboarding(OnboardingExitInfo.Simple) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(
            route = OnboardingNavRoute.PlusUpgrade.route,
            arguments = listOf(
                navArgument(OnboardingNavRoute.PlusUpgrade.sourceArgumentKey) {
                    type = NavType.EnumType(OnboardingUpgradeSource::class.java)
                    /* Set default value for onboarding flows with startDestination. */
                    when (flow) {
                        is OnboardingFlow.PatronAccountUpgrade,
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.Upsell,
                        is OnboardingFlow.UpsellSuggestedFolder,
                        -> {
                            defaultValue = flow.source
                        }

                        // Not a startDestination, default value should not be set.
                        is OnboardingFlow.AccountEncouragement,
                        is OnboardingFlow.EngageSdk,
                        is OnboardingFlow.InitialOnboarding,
                        is OnboardingFlow.LoggedOut,
                        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.ReferralLoginOrSignUp,
                        is OnboardingFlow.Welcome,
                        -> Unit
                    }
                },
                navArgument(OnboardingNavRoute.PlusUpgrade.forcePurchaseArgumentKey) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { navBackStackEntry ->
            val upgradeSource = navBackStackEntry.arguments
                ?.getSerializableCompat(OnboardingNavRoute.PlusUpgrade.sourceArgumentKey, OnboardingUpgradeSource::class.java)
                ?: throw IllegalStateException("Missing upgrade source argument")

            val forcePurchase = navBackStackEntry.arguments
                ?.getBoolean(OnboardingNavRoute.PlusUpgrade.forcePurchaseArgumentKey)
                ?: throw IllegalStateException("Missing force purchase argument")

            val userCreatedNewAccount = when (upgradeSource) {
                OnboardingUpgradeSource.ACCOUNT_DETAILS,
                OnboardingUpgradeSource.APPEARANCE,
                OnboardingUpgradeSource.ICONS,
                OnboardingUpgradeSource.THEMES,
                OnboardingUpgradeSource.BANNER_AD,
                OnboardingUpgradeSource.BOOKMARKS,
                OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                OnboardingUpgradeSource.END_OF_YEAR,
                OnboardingUpgradeSource.FILES,
                OnboardingUpgradeSource.FOLDERS,
                OnboardingUpgradeSource.SUGGESTED_FOLDERS,
                OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN,
                OnboardingUpgradeSource.HEADPHONE_CONTROLS_SETTINGS,
                OnboardingUpgradeSource.LOGIN,
                OnboardingUpgradeSource.LOGIN_PLUS_PROMOTION,
                OnboardingUpgradeSource.OVERFLOW_MENU,
                OnboardingUpgradeSource.PLUS_DETAILS,
                OnboardingUpgradeSource.PROFILE,
                OnboardingUpgradeSource.SKIP_CHAPTERS,
                OnboardingUpgradeSource.SETTINGS,
                OnboardingUpgradeSource.SLUMBER_STUDIOS,
                OnboardingUpgradeSource.UP_NEXT_SHUFFLE,
                OnboardingUpgradeSource.GENERATED_TRANSCRIPTS,
                OnboardingUpgradeSource.DEEP_LINK,
                OnboardingUpgradeSource.UNKNOWN,
                -> false

                OnboardingUpgradeSource.RECOMMENDATIONS -> true
            }

            OnboardingUpgradeFlow(
                viewModel = featuresViewModel,
                state = state,
                flow = flow,
                source = upgradeSource,
                isLoggedIn = signInState.isSignedIn,
                forcePurchase = forcePurchase,
                onBackPressed = {
                    if (userCreatedNewAccount) {
                        navController.popBackStack()
                    } else {
                        exitOnboarding(OnboardingExitInfo.Simple)
                    }
                },
                onNeedLogin = { navController.navigate(OnboardingNavRoute.logInOrSignUp) },
                onProceed = {
                    if (userCreatedNewAccount || forcePurchase) {
                        navController.navigate(OnboardingNavRoute.welcome)
                    } else {
                        finishOnboardingFlow()
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.welcome) {
            OnboardingWelcomePage(
                theme = theme,
                flow = flow,
                isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
                onDone = {
                    finishOnboardingFlow()
                },
                onContinueToDiscover = completeOnboardingToDiscover,
                onImportTapped = { navController.navigate(OnboardingImportFlow.route) },
                onBackPressed = {
                    // Don't allow navigation back to the upgrade screen after the user upgrades
                    if (signInState.isSignedInAsPlusOrPatron) {
                        finishOnboardingFlow()
                    } else {
                        navController.popBackStack()
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }
    }
}

private fun onLoginToExistingAccount(
    flow: OnboardingFlow,
    subscription: Subscription?,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    navController: NavHostController,
) {
    when (flow) {
        is OnboardingFlow.AccountEncouragement,
        is OnboardingFlow.InitialOnboarding,
        is OnboardingFlow.LoggedOut,
        is OnboardingFlow.EngageSdk,
        -> exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)

        is OnboardingFlow.ReferralLoginOrSignUp -> exitOnboarding(OnboardingExitInfo.Simple)

        // this should never happens, login is not initiated from welcome screen
        is OnboardingFlow.Welcome -> Unit

        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PatronAccountUpgrade,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        is OnboardingFlow.Upsell,
        is OnboardingFlow.UpsellSuggestedFolder,
        -> {
            if (subscription == null) {
                navController.navigate(OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.LOGIN)) {
                    // clear backstack after successful login
                    popUpTo(OnboardingNavRoute.logInOrSignUp) { inclusive = true }
                }
            } else {
                val exitInfo = if (flow is OnboardingFlow.UpsellSuggestedFolder) {
                    OnboardingExitInfo.ApplySuggestedFolders(flow.action)
                } else {
                    OnboardingExitInfo.Simple
                }
                exitOnboarding(exitInfo)
            }
        }
    }
}

@VisibleForTesting
object OnboardingNavRoute {

    const val createFreeAccount = "create_free_account"
    const val encourageFreeAccount = "encourage_free_account"
    const val forgotPassword = "forgot_password"
    const val logIn = "log_in"
    const val logInOrSignUp = "log_in_or_sign_up"
    const val welcome = "welcome"

    object PlusUpgrade {
        private const val routeBase = "plus_upgrade"

        const val sourceArgumentKey = "source"
        const val forcePurchaseArgumentKey = "force_purchase"

        // The route variable should only be used to navigate to the PlusUpgrade screens
        // when they are the startDestination and the args for these startDestinations are set using default values.
        // They are parsed based on this deep-link-like route by the navigation component.
        // For more details check here: https://developer.android.com/jetpack/compose/navigation#nav-with-args
        // In all other cases, use the routeWithSource function.
        const val route = "$routeBase/{$sourceArgumentKey}?$forcePurchaseArgumentKey={$forcePurchaseArgumentKey}"

        fun routeWithSource(
            source: OnboardingUpgradeSource,
            forcePurchase: Boolean = false,
        ) = "$routeBase/$source?$forcePurchaseArgumentKey=$forcePurchase"
    }
}

private val forcedPurchaseSources = listOf(
    OnboardingUpgradeSource.BOOKMARKS,
    OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
    OnboardingUpgradeSource.GENERATED_TRANSCRIPTS,
    OnboardingUpgradeSource.SKIP_CHAPTERS,
    OnboardingUpgradeSource.SUGGESTED_FOLDERS,
    OnboardingUpgradeSource.THEMES,
    OnboardingUpgradeSource.UP_NEXT_SHUFFLE,
)
