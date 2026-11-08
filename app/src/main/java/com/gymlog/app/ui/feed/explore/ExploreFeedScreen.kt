package com.gymlog.app.ui.feed.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.model.User
import com.gymlog.app.ui.common.FeedContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFeedScreen(
    onWorkoutClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onSwitchToFollowing: () -> Unit,
    viewModel: ExploreFeedViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSearchResults()
        viewModel.refreshFollowingIds()
        onPauseOrDispose { }
    }

    BackHandler(enabled = state.isSearchActive) {
        viewModel.onSearchToggle()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (!state.isSearchActive) {
                TopAppBar(
                    windowInsets = WindowInsets(0),
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text(
                                "Explorar",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Entrenos de la comunidad",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSwitchToFollowing) {
                            Icon(Icons.Default.People, contentDescription = "Ir a Seguidos")
                        }
                        IconButton(onClick = viewModel::onSearchToggle) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isSearchActive) {
            UserSearchContent(
                query = state.searchQuery,
                users = state.searchedUsers,
                isSearching = state.isSearchingUsers,
                followingIds = state.myFollowingIds,
                onQueryChange = viewModel::onSearchQueryChange,
                onClose = viewModel::onSearchToggle,
                onUserClick = onUserClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            FeedContent(
                workouts = state.workouts,
                isLoading = state.isLoading,
                error = state.error,
                weightUnit = state.weightUnit,
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onRefresh,
                onWorkoutClick = onWorkoutClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun UserSearchContent(
    query: String,
    users: List<User>,
    isSearching: Boolean,
    followingIds: Collection<String>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onClose = onClose
        )

        Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        when {
            users.isNotEmpty() -> {
                Text(
                    text = if (users.size == 1) "1 resultado" else "${users.size} resultados",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(users, key = { it.uid }) { user ->
                        UserSearchItem(
                            user = user,
                            query = query,
                            isFollowing = followingIds.contains(user.uid),
                            onClick = { onUserClick(user.uid) }
                        )
                    }
                }
            }

            isSearching -> Unit

            query.isBlank() -> SearchPlaceholder(
                icon = Icons.Default.PersonSearch,
                title = "Encuentra a otros atletas",
                subtitle = "Busca por nombre de usuario para ver sus entrenos"
            )

            else -> SearchPlaceholder(
                icon = Icons.Default.SearchOff,
                title = "Sin resultados",
                subtitle = "No hay usuarios que coincidan con \"${query.trim()}\""
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Buscar usuarios") },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar búsqueda")
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar texto")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
    )
}

@Composable
private fun SearchPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UserSearchItem(
    user: User,
    query: String,
    isFollowing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user = user)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightedName(user.username, query),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (user.followersCount == 1) "1 seguidor" else "${user.followersCount} seguidores",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isFollowing) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Siguiendo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(user: User) {
    val avatarModifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)

    if (user.photoUrl.isBlank()) {
        Box(modifier = avatarModifier, contentAlignment = Alignment.Center) {
            Text(
                text = user.username.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = "Foto de ${user.username}",
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    }
}

@Composable
private fun highlightedName(name: String, query: String): AnnotatedString {
    val q = query.trim()
    val start = if (q.isEmpty()) -1 else name.indexOf(q, ignoreCase = true)
    val highlight = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        if (start < 0) {
            append(name)
        } else {
            append(name.substring(0, start))
            withStyle(SpanStyle(color = highlight)) {
                append(name.substring(start, start + q.length))
            }
            append(name.substring(start + q.length))
        }
    }
}