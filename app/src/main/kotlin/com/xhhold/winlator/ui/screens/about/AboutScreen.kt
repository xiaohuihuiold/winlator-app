@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val navController = LocalNavController.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate("home") {
                popUpTo("about") {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    BackHandler {
        navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LinkText(
                            text = "winlator.org",
                            onClick = { uriHandler.openUri("https://www.winlator.org") },
                        )
                        Text(
                            text = "${stringResource(R.string.version)} $versionName",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.credits_and_third_party_apps),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            credits.forEach { credit ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(credit.title, style = MaterialTheme.typography.bodyLarge)
                        LinkText(text = credit.label, onClick = { uriHandler.openUri(credit.url) })
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LinkText(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private data class CreditLink(
    val title: String,
    val label: String,
    val url: String,
)

private val credits = listOf(
    CreditLink("GLIBC Patches", "Termux Pacman", "https://github.com/termux-pacman/glibc-packages"),
    CreditLink("Wine", "winehq.org", "https://www.winehq.org"),
    CreditLink("Box86/Box64", "ptitseb", "https://github.com/ptitSeb"),
    CreditLink("Mesa (Turnip/Zink/VirGL)", "mesa3d.org", "https://www.mesa3d.org"),
    CreditLink("DXVK", "github.com/doitsujin/dxvk", "https://github.com/doitsujin/dxvk"),
    CreditLink("VKD3D", "gitlab.winehq.org/wine/vkd3d", "https://gitlab.winehq.org/wine/vkd3d"),
    CreditLink("CNC DDraw", "github.com/FunkyFr3sh/cnc-ddraw", "https://github.com/FunkyFr3sh/cnc-ddraw"),
)
