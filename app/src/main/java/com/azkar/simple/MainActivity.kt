package com.azkar.simple

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// DataStore extension
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "azkar_prefs")

// Data class for Zikr
data class Zikr(
    val id: Int,
    val text: String,
    val repeat: Int,
    val source: String,
    val virtue: String
)

// ViewModel
class AzkarViewModel : ViewModel() {
    private lateinit var dataStore: DataStore<Preferences>
    
    fun setDataStore(ds: DataStore<Preferences>) {
        dataStore = ds
    }
    
    fun getProgress(id: Int): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey("zikr_$id")] ?: 0
        }
    }
    
    suspend fun incrementProgress(id: Int, maxCount: Int) {
        dataStore.edit { preferences ->
            val currentCount = preferences[intPreferencesKey("zikr_$id")] ?: 0
            if (currentCount < maxCount) {
                preferences[intPreferencesKey("zikr_$id")] = currentCount + 1
            }
        }
    }
    
    suspend fun resetProgress(id: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("zikr_$id")] = 0
        }
    }
    
    suspend fun resetAllProgress(azkarList: List<Zikr>) {
        dataStore.edit { preferences ->
            azkarList.forEach { zikr ->
                preferences[intPreferencesKey("zikr_${zikr.id}")] = 0
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colors = lightColors(
                    primary = Color(0xFF4CAF50),
                    primaryVariant = Color(0xFF388E3C),
                    secondary = Color(0xFF81C784)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AzkarApp()
                }
            }
        }
    }
}

@Composable
fun AzkarApp() {
    val context = LocalContext.current
    val viewModel: AzkarViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        viewModel.setDataStore(context.dataStore)
    }
    
    val azkarList = remember { loadAzkarFromAssets(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "أذكار الصباح",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            backgroundColor = Color(0xFF4CAF50),
            contentColor = Color.White
        )
        
        // Progress indicator
        OverallProgress(azkarList, viewModel)
        
        // Reset all button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        viewModel.resetAllProgress(azkarList)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF388E3C))
            ) {
                Text("إعادة تعيين الكل", color = Color.White)
            }
        }
        
        // Azkar list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(azkarList) { zikr ->
                ZikrCard(zikr = zikr, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OverallProgress(azkarList: List<Zikr>, viewModel: AzkarViewModel) {
    var totalCompleted by remember { mutableStateOf(0) }
    val totalAzkar = azkarList.size
    
    LaunchedEffect(azkarList) {
        var completed = 0
        azkarList.forEach { zikr ->
            viewModel.getProgress(zikr.id).collect { progress ->
                if (progress >= zikr.repeat) {
                    completed++
                }
                totalCompleted = completed
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "التقدم الإجمالي",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (totalAzkar > 0) totalCompleted.toFloat() / totalAzkar else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalCompleted / $totalAzkar",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ZikrCard(zikr: Zikr, viewModel: AzkarViewModel) {
    val progress by viewModel.getProgress(zikr.id).collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val isCompleted = progress >= zikr.repeat
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isCompleted) Color(0xFFE8F5E8) else Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Zikr text
            Text(
                text = zikr.text,
                fontSize = 16.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Source
            if (zikr.source.isNotEmpty()) {
                Text(
                    text = "المصدر: ${zikr.source}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Progress and buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Buttons
                Row {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.resetProgress(zikr.id)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("إعادة", color = Color.White, fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.incrementProgress(zikr.id, zikr.repeat)
                            }
                        },
                        enabled = !isCompleted,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isCompleted) Color.Gray else Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = if (isCompleted) "تم" else "ذكر",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Progress text
                Text(
                    text = "$progress / ${zikr.repeat}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color(0xFF4CAF50) else Color.Black
                )
            }
            
            // Progress bar
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (zikr.repeat > 0) progress.toFloat() / zikr.repeat else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4CAF50)
            )
        }
    }
}

fun loadAzkarFromAssets(context: Context): List<Zikr> {
    return try {
        val inputStream = context.assets.open("adhkar_sabah.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(json)
        
        val azkarList = mutableListOf<Zikr>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            azkarList.add(
                Zikr(
                    id = jsonObject.getInt("id"),
                    text = jsonObject.getString("text"),
                    repeat = jsonObject.getInt("repeat"),
                    source = jsonObject.optString("source", ""),
                    virtue = jsonObject.optString("virtue", "")
                )
            )
        }
        azkarList
    } catch (e: IOException) {
        emptyList()
    }
}

