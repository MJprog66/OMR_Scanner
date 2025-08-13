package thesis.project.omrscanner.create

import androidx.compose.runtime.*
import androidx.navigation.NavHostController

@Composable
fun CreateScreen(navController: NavHostController) {
    var step by remember { mutableStateOf(CreateStep.Options) }

    when (step) {
        CreateStep.Options -> CreateOptionsScreen(
            onCreateClick = { step = CreateStep.PortraitConfig }, // Directly go to portrait config
            onViewClick = { step = CreateStep.ViewCreated },
            onBack = { navController.popBackStack() }
        )

        CreateStep.PortraitConfig -> PortraitOMRConfigScreen(
            onSave = { step = CreateStep.ViewCreated },
            onBack = { step = CreateStep.Options }
        )

        CreateStep.ViewCreated -> ViewCreatedOMRsScreen(
            onBack = { step = CreateStep.Options }
        )
    }
}

enum class CreateStep {
    Options,
    PortraitConfig,
    ViewCreated
}
