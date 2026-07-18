package com.vertice.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            VerticeApp()
        }
    }
}

@Composable
fun VerticeApp() {

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B0D10)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),

                verticalArrangement =
                    Arrangement.spacedBy(20.dp)
            ) {

                Text(
                    text = "VÉRTICE",

                    color = Color(0xFF8B949E),

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "Nova Empresa",

                    color = Color(0xFFF1F3F5),

                    fontSize = 30.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "Ano 1 · Semana 1",

                    color = Color(0xFF8B949E),

                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    MetricCard(
                        title = "CAIXA",

                        value = "$100.000",

                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "REPUTAÇÃO",

                        value = "10",

                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "◇",

                        color = Color(0xFFC9F27C),

                        fontSize = 54.sp
                    )

                    Text(
                        text = "O futuro ainda não foi construído.",

                        color = Color(0xFFF1F3F5),

                        fontSize = 20.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Crie sua primeira tecnologia e transforme uma ideia em uma empresa.",

                        color = Color(0xFF8B949E),

                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                color = Color(0xFF12161B),

                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = title,

            color = Color(0xFF8B949E),

            fontSize = 11.sp
        )

        Spacer(
            modifier = Modifier.size(8.dp)
        )

        Text(
            text = value,

            color = Color(0xFFF1F3F5),

            fontSize = 20.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}
