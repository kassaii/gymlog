
package com.gymlog.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymlog.app.data.model.User

@Composable
fun UserSearchView(
    query: String,
    users: List<User>,
    isSearching: Boolean,
    followingIds: Collection<String>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onClose = onClose
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {
            if (isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        when {
            users.isNotEmpty() -> {
                Text(
                    text = if (users.size == 1) {
                        "1 resultado"
                    } else {
                        "${users.size} resultados"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = users,
                        key = { it.uid }
                    ) { user ->
                        UserRow(
                            user = user,
                            isFollowing = followingIds.contains(user.uid),
                            onClick = {
                                onUserClick(user.uid)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.3f
                            )
                        )
                    }
                }
            }

            isSearching -> Unit

            query.isBlank() -> {
                EmptyStateView(
                    icon = Icons.Default.PersonSearch,
                    title = "Encuentra a otros atletas",
                    subtitle = "Busca por nombre de usuario para ver sus entrenos"
                )
            }

            else -> {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "Sin resultados",
                    subtitle = "No hay usuarios que coincidan con \"${query.trim()}\""
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text("Buscar usuarios")
        },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cerrar búsqueda"
                )
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar texto"
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
    )
}
