package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PollOption(val id: String, val text: String, val votes: Int = 0)

data class PollData(
    val pollId: String,
    val question: String,
    val options: List<PollOption>
)

sealed interface VotingUiState {
    data object Loading : VotingUiState
    data class Success(
        val poll: PollData, 
        val hasVoted: Boolean, 
        val totalVotes: Int, 
        val userVoteId: String?
    ) : VotingUiState
    data object NoActivePoll : VotingUiState
    data class Error(val message: String) : VotingUiState
}

@HiltViewModel
class VotingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val FIREBASE_PROJECT_ID = "avidtunevotingtest1"
    private val FIREBASE_APP_ID = "1:951098213183:android:99f2ade651ca76ee6ac98a"
    private val FIREBASE_API_KEY = "AIzaSyDNcvfk4nUbJN3UkACvcj-k4hddQSkO0is"
    private val FIREBASE_DATABASE_URL = "https://avidtunevotingtest1-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val _uiState = MutableStateFlow<VotingUiState>(VotingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    init {
        initFirebase()
    }

    private fun initFirebase() {
        try {
            // Manual initialization bypasses the need for google-services.json
            // Prevents crashes in CI/CD and open-source builds without keys
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .setApplicationId(FIREBASE_APP_ID)
                    .setApiKey(FIREBASE_API_KEY)
                    .setDatabaseUrl(FIREBASE_DATABASE_URL)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            
            auth = FirebaseAuth.getInstance()
            db = FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL)
            
            authenticateAndListen()
        } catch (e: Exception) {
            _uiState.value = VotingUiState.Error("Firebase Init Error: ${e.message}")
        }
    }

    private fun authenticateAndListen() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listenToPoll()
                } else {
                    _uiState.value = VotingUiState.Error("Failed to connect to voting server: ${task.exception?.message}")
                }
            }
        } else {
            listenToPoll()
        }
    }

    private fun listenToPoll() {
        db.reference.child("polls/current").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    _uiState.value = VotingUiState.NoActivePoll
                    return
                }

                val pollId = snapshot.child("id").getValue(String::class.java) ?: return
                val question = snapshot.child("question").getValue(String::class.java) ?: return
                
                val optionsSnapshot = snapshot.child("options")
                val baseOptions = optionsSnapshot.children.mapNotNull { optSnap ->
                    val id = optSnap.key ?: return@mapNotNull null
                    val text = optSnap.getValue(String::class.java) ?: return@mapNotNull null
                    PollOption(id, text, 0)
                }

                listenToVotes(pollId, question, baseOptions)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = VotingUiState.Error(error.message)
            }
        })
    }

    private fun listenToVotes(pollId: String, question: String, baseOptions: List<PollOption>) {
        db.reference.child("votes").child(pollId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = auth.currentUser?.uid ?: return
                var userVoteId: String? = null
                val voteCounts = mutableMapOf<String, Int>()

                // Count votes safely
                for (voteSnap in snapshot.children) {
                    val votedOptionId = voteSnap.getValue(String::class.java) ?: continue
                    voteCounts[votedOptionId] = (voteCounts[votedOptionId] ?: 0) + 1
                    
                    if (voteSnap.key == uid) {
                        userVoteId = votedOptionId
                    }
                }

                // Append counts back to options & sort
                val finalOptions = baseOptions.map {
                    it.copy(votes = voteCounts[it.id] ?: 0)
                }.sortedByDescending { it.votes }

                val totalVotes = finalOptions.sumOf { it.votes }

                _uiState.value = VotingUiState.Success(
                    poll = PollData(pollId, question, finalOptions),
                    hasVoted = userVoteId != null,
                    totalVotes = totalVotes,
                    userVoteId = userVoteId
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = VotingUiState.Error(error.message)
            }
        })
    }

    fun submitVote(pollId: String, optionId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.reference.child("votes").child(pollId).child(uid).setValue(optionId)
    }
}