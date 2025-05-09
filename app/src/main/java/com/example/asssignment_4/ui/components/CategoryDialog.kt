package com.example.asssignment_4.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.asssignment_4.R
import com.example.asssignment_4.model.Gene
import kotlinx.coroutines.launch

@Composable
fun CategoryDialog(
    categories: List<Gene>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    Log.d("ArtistDetailScreen", "CategoryDialog: isLoading=$isLoading, categories=${categories.size}")

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .fillMaxWidth(1f)
                .heightIn(min = 550.dp, max = 595.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 16.dp),
                    fontWeight = FontWeight(499)
                )
                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Loading",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        categories.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No categories available", color = Color.Gray)
                            }
                        }
                        else -> {
                            val navigateToPrevious: () -> Unit = {
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else categories.size - 1
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(currentIndex)
                                }
                            }
                            val navigateToNext: () -> Unit = {
                                currentIndex = if (currentIndex < categories.size - 1) currentIndex + 1 else 0
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(currentIndex)
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyRow(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .width(480.dp)
                                        .padding(horizontal = 25.dp),
                                    contentPadding = PaddingValues(
                                        start = 15.dp,
                                        end = 12.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(categories.size) { index ->
                                        val gene = categories[index]
                                        Card(
                                            modifier = Modifier
                                                .fillParentMaxHeight(0.95f)
                                                .width(240.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                AsyncImage(
                                                    model = gene.links?.thumbnail?.href,
                                                    contentDescription = gene.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(0.35f),
                                                    contentScale = ContentScale.Crop,
                                                    placeholder = painterResource(id = R.drawable.artsy_logo),
                                                    error = painterResource(id = R.drawable.artsy_logo)
                                                )

                                                Text(
                                                    text = gene.displayName ?: gene.name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 19.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp)
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(0.65f)
                                                        .fillMaxWidth()
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = gene.description ?: "No description available.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight(499),
                                                        textAlign = TextAlign.Start
                                                    )
                                                    Spacer(Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = navigateToPrevious,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 7.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous Category",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = navigateToNext,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next Category",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row (modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }
}