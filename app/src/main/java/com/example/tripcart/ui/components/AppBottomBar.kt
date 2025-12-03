package com.example.tripcart.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val iconDrawable: Int? = null
) {
    object ActiveList : BottomNavItem(
        route = "active_list",
        title = "진행 중",
        icon = Icons.Default.PlayArrow
    )
    
    object List : BottomNavItem(
        route = "list",
        title = "전체",
        iconDrawable = R.drawable.folder
    )
    
    object Map : BottomNavItem(
        route = "map",
        title = "지도",
        iconDrawable = R.drawable.map
    )
    
    object Ranking : BottomNavItem(
        route = "ranking",
        title = "랭킹",
        iconDrawable = R.drawable.trophy
    )
    
    object MyPage : BottomNavItem(
        route = "my_page",
        title = "마이페이지",
        icon = Icons.Default.Person
    )
}

@Composable
fun AppBottomBar(
    currentRoute: String? = null,
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.ActiveList,
        BottomNavItem.List,
        BottomNavItem.Map,
        BottomNavItem.Ranking,
        BottomNavItem.MyPage
    )
    
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { onItemClick(item.route) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        when {
                            item.iconDrawable != null -> {
                                Image(
                                    painter = painterResource(id = item.iconDrawable),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(30.dp),
                                    colorFilter = ColorFilter.tint(
                                        if (isSelected) {
                                            PrimaryAccent
                                        } else {
                                            Color.Black.copy(alpha = 0.3f)
                                        }
                                    )
                                )
                            }
                            item.icon != null -> {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(30.dp),
                                    tint = if (isSelected) {
                                        PrimaryAccent
                                    } else {
                                        Color.Black.copy(alpha = 0.3f)
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            PrimaryAccent
                        } else {
                            Color.Black.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

