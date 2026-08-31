package com.cgens67.avidtune.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.avidtune.R
import com.cgens67.avidtune.viewmodels.VotingUiState
import com.cgens67.avidtune.viewmodels.VotingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    navController: NavController,
    viewModel: VotingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Community Poll", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = uiState, label = "VotingState") { state ->
                when (state) {
                    is VotingUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is VotingUiState.Error -> {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.error), 
                                null, 
                                tint = MaterialTheme.colorScheme.error, 
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Oops!", style = MaterialTheme.typography.headlineSmall)
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                    is VotingUiState.NoActivePoll -> {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.info), 
                                null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No Active Polls", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Check back later to vote on new features!", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                    is VotingUiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = state.poll.question,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${state.totalVotes} total votes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            items(state.poll.options) { option ->
                                val isUserVote = state.userVoteId == option.id
                                val percentage = if (state.totalVotes > 0) option.votes.toFloat() / state.totalVotes else 0f
                                
                                val animatedProgress by animateFloatAsState(
                                    targetValue = percentage,
                                    animationSpec = tween(1200, easing = FastOutSlowInEasing),
                                    label = "progress"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable(enabled = !state.hasVoted) {
                                            viewModel.submitVote(state.poll.pollId, option.id)
                                        }
                                ) {
                                    if (state.hasVoted) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedProgress.coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(
                                                    if (isUserVote) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                                )
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isUserVote) {
                                                Icon(
                                                    painterResource(R.drawable.check_circle),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                                                )
                                            }
                                            Text(
                                                text = option.text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isUserVote) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isUserVote) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (state.hasVoted) {
                                            Text(
                                                text = "${(percentage * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUserVote) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (state.hasVoted) {
                                item {
                                    Spacer(Modifier.height(32.dp))
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Thanks for voting!",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}