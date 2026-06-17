package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.BuildConfig
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.utils.backToMain

// ── Shimmer brush ──────────────────────────────────────────────────────────

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.0f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.0f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim, y = 0f),
        end = Offset(x = translateAnim + 300f, y = 0f),
    )
}

// ── Data ───────────────────────────────────────────────────────────────────

private data class Contributor(
    val avatarUrl: String,
    val name: Int,
    val role: Int,
    val profileUrl: String,
)

private data class SocialLink(
    val iconRes: Int,
    val url: String,
    val labelRes: Int,
)

private val developers = listOf(
    Contributor(
        avatarUrl = "https://avatars.githubusercontent.com/u/237695506?v=4",
        name = R.string.app_name, // Using a fallback since the name isn't dynamic (or we hardcode "cgens67" as string)
        role = R.string.lead_developer,
        profileUrl = "https://github.com/cgens67",
    ),
    Contributor(
        avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/610099055_17913713952255174_103251256549509672_n.jpg",
        name = R.string.co_developer, 
        role = R.string.co_developer,
        profileUrl = "https://www.instagram.com/chinsiang0304",
    )
)

private val supporters = listOf(
    Contributor(
        avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/avidtunescreenshots/723961601_17939287968252146_3708914078912691512_n.jpg",
        name = R.string.supporter, 
        role = R.string.supporter,
        profileUrl = "https://www.instagram.com/illit._.610",
    ),
    Contributor(
        avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/627632846_17942422653109085_6346845649854714491_n.jpg",
        name = R.string.supporter, 
        role = R.string.supporter,
        profileUrl = "https://www.instagram.com/wei_.3120",
    )
)

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
            ),
        ) {

            // ── Hero card ─────────────────────────────────────────────────
            item {
                HeroCard(shimmerBrush = shimmerEffect())
            }

            // ── About App card ────────────────────────────────────────────
            item {
                AboutAppCard()
            }

            // ── Social card ───────────────────────────────────────────────
            item {
                SocialCard(
                    links = listOf(
                        SocialLink(R.drawable.github,    "https://github.com/cgens67/AvidTune", R.string.social_github),
                        SocialLink(R.drawable.telegram,  "https://t.me/avidtuneupdates",       R.string.social_telegram),
                        SocialLink(R.drawable.resource_public, "https://avidtune.vercel.app/", R.string.social_web),
                    ),
                    onLinkClick = { uriHandler.openUri(it) },
                )
            }

            // ── Developers section ───────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.developers), iconRes = R.drawable.person)
            }

            item {
                ContributorsCard(
                    contributors = listOf(
                        Contributor(
                            avatarUrl = "https://avatars.githubusercontent.com/u/237695506?v=4",
                            name = 0, // Using raw string name
                            role = R.string.lead_developer,
                            profileUrl = "https://github.com/cgens67",
                        ),
                        Contributor(
                            avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/610099055_17913713952255174_103251256549509672_n.jpg",
                            name = 1,
                            role = R.string.co_developer,
                            profileUrl = "https://www.instagram.com/chinsiang0304",
                        )
                    ),
                    onContributorClick = { uriHandler.openUri(it) },
                    rawNames = listOf("cgens67", "chinsiang0304")
                )
            }

            // ── Supporters section ───────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.supporters), iconRes = R.drawable.favorite)
            }

            item {
                ContributorsCard(
                    contributors = listOf(
                        Contributor(
                            avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/706901250_17936492904252146_770663282096616853_n.jpg",
                            name = 0,
                            role = R.string.supporter,
                            profileUrl = "https://www.instagram.com/illit._.610",
                        ),
                        Contributor(
                            avatarUrl = "https://raw.githubusercontent.com/cgens67/Assets/refs/heads/main/627632846_17942422653109085_6346845649854714491_n.jpg",
                            name = 1,
                            role = R.string.supporter,
                            profileUrl = "https://www.instagram.com/wei_.3120",
                        )
                    ),
                    onContributorClick = { uriHandler.openUri(it) },
                    rawNames = listOf("illit._.610", "wei_.3120")
                )
            }

            // ── License footer ────────────────────────────────────────────
            item {
                LicenseFooter(
                    onLicenseClick = {
                        uriHandler.openUri("https://github.com/cgens67/AvidTune/blob/master/LICENSE")
                    }
                )
            }
        }
    }
}

// ── Hero card ──────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(shimmerBrush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App icon with shimmer
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.avidtune_monochrome),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            BlendMode.SrcIn,
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    // Shimmer overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(shimmerBrush),
                    )
                }
            }

            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Version + build badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VersionBadge(
                    text = "v${BuildConfig.VERSION_NAME}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                VersionBadge(
                    text = "${BuildConfig.VERSION_CODE}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (BuildConfig.DEBUG) {
                    VersionBadge(
                        text = "DEBUG",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            // Dev credit row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    AsyncImage(
                        model = "https://avatars.githubusercontent.com/u/237695506?v=4",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.dev_by_cgens67),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.gpl_3_license),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── About App card ─────────────────────────────────────────────────────────

@Composable
private fun AboutAppCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.about_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.about_app_desc_part1))
                    val link = LinkAnnotation.Url("https://ganvo.vercel.app/")
                    withLink(link) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.about_app_desc_ganvo))
                        }
                    }
                    append(stringResource(R.string.about_app_desc_part2))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Social card ────────────────────────────────────────────────────────────

@Composable
private fun SocialCard(
    links: List<SocialLink>,
    onLinkClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.social_links),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Social icons grid — two rows of 3 (or as many as available)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links.chunked(3).forEach { rowLinks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowLinks.forEach { link ->
                            SocialPill(
                                iconRes = link.iconRes,
                                label = stringResource(link.labelRes),
                                onClick = { onLinkClick(link.url) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill remaining cells if row is incomplete
                        repeat(3 - rowLinks.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Generic Section Header ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, iconRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Contributors Card ──────────────────────────────────────────────────────

@Composable
private fun ContributorsCard(
    contributors: List<Contributor>,
    rawNames: List<String>,
    onContributorClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            contributors.forEachIndexed { index, contributor ->
                ContributorRow(
                    contributor = contributor,
                    rawName = rawNames[index],
                    onClick = { onContributorClick(contributor.profileUrl) },
                )
                if (index < contributors.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

// ── Contributor row ────────────────────────────────────────────────────────

@Composable
private fun ContributorRow(
    contributor: Contributor,
    rawName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            AsyncImage(
                model = contributor.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        }

        // Name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rawName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(contributor.role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Forward arrow
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── License footer ─────────────────────────────────────────────────────────

@Composable
private fun LicenseFooter(onLicenseClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onLicenseClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.policy),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gpl_3_license),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.view_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Small helpers ──────────────────────────────────────────────────────────

@Composable
private fun VersionBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SocialPill(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = modifier.height(48.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
