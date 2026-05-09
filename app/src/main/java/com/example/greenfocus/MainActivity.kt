package com.example.greenfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.greenfocus.ui.theme.GreenFocusTheme
import androidx.navigation.compose.rememberNavController
import com.example.greenfocus.ui.navigation.SetupNavGraph
import com.example.greenfocus.ui.screen.store.StoreScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenFocusTheme {
                GreenFocusTheme {
                    val navController = rememberNavController()
                    SetupNavGraph(navController = navController)
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GreenFocusTheme {
        Greeting("Android")
    }
}

