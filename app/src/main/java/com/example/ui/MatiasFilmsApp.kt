package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Client
import com.example.data.Video
import com.example.data.Week
import com.example.ui.theme.*

// Formats content for WhatsApp / Copy Summary
fun formatWhatsAppMessage(client: Client, week: Week, videos: List<Video>): String {
    val done = videos.count { it.status == "terminado" }
    val total = videos.size
    val pct = if (total > 0) (done * 100) / total else 0

    val sb = StringBuilder()
    sb.append("Hola *${client.name.split(" ").firstOrNull() ?: client.name}*! 👋\n")
    sb.append("Te mando el seguimiento de tu proyecto en *Matías Films*.\n\n")
    sb.append("📊 *${week.label}* — Progress: *$pct% completado* ($done/$total videos)\n")
    sb.append("───────────────────\n")

    videos.forEachIndexed { i, v ->
        val emoji = when (v.status) {
            "terminado" -> "🟢"
            "en-proceso" -> "🟡"
            "revision" -> "🔵"
            else -> "⬜"
        }
        val statusName = when (v.status) {
            "terminado" -> "Terminado"
            "en-proceso" -> "En proceso"
            "revision" -> "En revisión"
            else -> "Pendiente"
        }
        val titleText = if (v.title.isBlank()) "Video ${i + 1}" else v.title
        sb.append("$emoji *$titleText* ($statusName)\n")
        if (v.note.isNotBlank()) {
            sb.append("   _Nota: ${v.note}_\n")
        }
        sb.append("\n")
    }
    
    return sb.toString().trim()
}

@Composable
fun MatiasFilmsApp(viewModel: ClientViewModel) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val weeks by viewModel.weeksOfSelectedClient.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val videos by viewModel.videosOfSelectedWeek.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showEditClientDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf<Video?>(null) }
    var showShareDialog by remember { mutableStateOf<Boolean>(false) }

    // Intercept back actions on Compact views when a client is selected
    if (selectedClient != null) {
        BackHandler {
            viewModel.selectClient(null)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = BrandBg
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth > 720.dp

            if (isExpanded) {
                // Side-by-Side Tablet / Desktop Mode
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar panel
                    SidebarPanel(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .background(BrandSurface)
                            .border(width = 1.dp, color = BrandBorder, shape = RoundedCornerShape(0.dp)),
                        clients = clients,
                        selectedClient = selectedClient,
                        onClientSelected = { viewModel.selectClient(it) },
                        onAddClientClick = { showAddClientDialog = true },
                        onLoadSamplesClick = { viewModel.loadSampleData() }
                    )

                    // Right content area
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedClient != null) {
                            ClientDetailsScreen(
                                client = selectedClient!!,
                                weeks = weeks,
                                selectedWeek = selectedWeek,
                                videos = videos,
                                onWeekSelected = { viewModel.selectWeek(it) },
                                onAddWeekClick = { viewModel.addWeekForSelectedClient() },
                                onEditClientClick = { showEditClientDialog = true },
                                onShareClick = { showShareDialog = true },
                                onVideoClick = { showVideoDialog = it }
                            )
                        } else {
                            NoClientSelectedState(
                                hasClients = clients.isNotEmpty(),
                                onAddClientClick = { showAddClientDialog = true },
                                onLoadSamplesClick = { viewModel.loadSampleData() }
                            )
                        }
                    }
                }
            } else {
                // Mobile stacked mode
                AnimatedContent(
                    targetState = selectedClient,
                    transitionSpec = {
                        if (targetState != null) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "Navigation"
                ) { currentClient ->
                    if (currentClient != null) {
                        ClientDetailsScreen(
                            client = currentClient,
                            weeks = weeks,
                            selectedWeek = selectedWeek,
                            videos = videos,
                            onWeekSelected = { viewModel.selectWeek(it) },
                            onAddWeekClick = { viewModel.addWeekForSelectedClient() },
                            onEditClientClick = { showEditClientDialog = true },
                            onShareClick = { showShareDialog = true },
                            onVideoClick = { showVideoDialog = it },
                            onBackClick = { viewModel.selectClient(null) }
                        )
                    } else {
                        MainMobileDashboard(
                            clients = clients,
                            onClientSelected = { viewModel.selectClient(it) },
                            onAddClientClick = { showAddClientDialog = true },
                            onLoadSamplesClick = { viewModel.loadSampleData() }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddClientDialog) {
        ClientConfigDialog(
            title = "Nuevo cliente",
            onDismiss = { showAddClientDialog = false },
            onSave = { name, videosRow, contentType, notes ->
                viewModel.createClient(name, videosRow, contentType, notes)
                showAddClientDialog = false
            }
        )
    }

    if (showEditClientDialog && selectedClient != null) {
        val client = selectedClient!!
        ClientConfigDialog(
            title = "Editar cliente",
            initialName = client.name,
            initialVideos = client.videosPerWeek,
            initialType = client.contentType,
            initialNotes = client.notes,
            onDismiss = { showEditClientDialog = false },
            onRemove = {
                viewModel.deleteClient(client.id)
                showEditClientDialog = false
            },
            onSave = { name, videosRow, contentType, notes ->
                viewModel.updateClient(client, name, videosRow, contentType, notes)
                showEditClientDialog = false
            }
        )
    }

    if (showVideoDialog != null) {
        val video = showVideoDialog!!
        VideoDetailsDialog(
            video = video,
            onDismiss = { showVideoDialog = null },
            onSave = { title, status, note ->
                viewModel.updateVideo(video, title, status, note)
                showVideoDialog = null
            }
        )
    }

    if (showShareDialog && selectedClient != null && selectedWeek != null) {
        ShareTrackerSummaryDialog(
            client = selectedClient!!,
            week = selectedWeek!!,
            videos = videos,
            onDismiss = { showShareDialog = false },
            context = context
        )
    }
}

// ──────────────────────────────────────────────────────
// SIDEBAR (TABLET PANEL)
// ──────────────────────────────────────────────────────
@Composable
fun SidebarPanel(
    modifier: Modifier = Modifier,
    clients: List<Client>,
    selectedClient: Client?,
    onClientSelected: (Client) -> Unit,
    onAddClientClick: () -> Unit,
    onLoadSamplesClick: () -> Unit
) {
    Column(modifier = modifier) {
        // Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            AppNameHeader()
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClientClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    contentColor = BrandBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("new_client_button_tablet")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo cliente", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
            }
        }

        HorizontalDivider(color = BrandBorder)

        // Client List
        if (clients.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Aún no tenés clientes.",
                        color = BrandTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Probar con demostración",
                        color = BrandAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onLoadSamplesClick() }
                            .padding(8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(clients) { client ->
                    val isSelected = client.id == selectedClient?.id
                    ClientListItem(
                        client = client,
                        isSelected = isSelected,
                        onClick = { onClientSelected(client) }
                    )
                }
            }
        }

        HorizontalDivider(color = BrandBorder)

        // Sidebar Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Matias Films Manager v1.0",
                color = BrandTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                "Base de datos local activa.",
                color = BrandTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// COMPACT MOBILE MAIN LIST / DASHBOARD
// ──────────────────────────────────────────────────────
@Composable
fun MainMobileDashboard(
    clients: List<Client>,
    onClientSelected: (Client) -> Unit,
    onAddClientClick: () -> Unit,
    onLoadSamplesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBg)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandSurface)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .border(width = 0.5.dp, color = BrandBorder)
        ) {
            AppNameHeader()
        }

        // Action Quick Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clientes (${clients.size})",
                color = BrandTextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Button(
                onClick = onAddClientClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    contentColor = BrandBg
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("new_client_button_mobile")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // List
        if (clients.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎬",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        "No tienes clientes todavía.",
                        color = BrandTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Crea tu primer cliente para organizar y controlar el progreso de sus videos.",
                        color = BrandTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddClientClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAccent,
                            contentColor = BrandBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Agregar nuevo cliente", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onLoadSamplesClick) {
                        Text("Cargar datos de ejemplo", color = BrandAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients) { client ->
                    ClientListItemMobile(
                        client = client,
                        onClick = { onClientSelected(client) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// COMPOSE BRAND HEADER (MATCHES SYNE & DESIGN ATMOSPHERE)
// ──────────────────────────────────────────────────────
@Composable
fun AppNameHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "Matías ",
            color = BrandTextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = "Films",
            color = BrandAccent,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ColorTerminado)
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Text(
            text = "GESTICIÓN DE CLIENTES",
            color = BrandTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}

// ──────────────────────────────────────────────────────
// INDIVIDUAL CLIENT LIST ITEMS (TABLET VS PHONE SIZES)
// ──────────────────────────────────────────────────────
@Composable
fun ClientListItem(
    client: Client,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) BrandSurfaceVariant else Color.Transparent
    val borderColor = if (isSelected) BrandBorderLight else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initial Avatar circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(getAvatarColor(client.colorIndex).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(client.name),
                color = getAvatarColor(client.colorIndex),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.name,
                color = BrandTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${client.videosPerWeek} videos/sem · ${client.contentType}",
                color = BrandTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ClientListItemMobile(
    client: Client,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = BrandSurface,
            contentColor = BrandTextPrimary
        ),
        border = BorderStroke(1.dp, BrandBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(client.colorIndex).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(client.name),
                    color = getAvatarColor(client.colorIndex),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${client.videosPerWeek} videos semanales",
                    color = BrandTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = client.contentType.ifBlank { "General" },
                    color = BrandTextMuted,
                    fontSize = 11.sp
                )
            }

            // Arrow Indicator
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandTextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// CLIENT DETAIL TRACKER VIEW
// ──────────────────────────────────────────────────────
@Composable
fun ClientDetailsScreen(
    client: Client,
    weeks: List<Week>,
    selectedWeek: Week?,
    videos: List<Video>,
    onWeekSelected: (Week) -> Unit,
    onAddWeekClick: () -> Unit,
    onEditClientClick: () -> Unit,
    onShareClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandSurface)
                .border(width = 0.5.dp, color = BrandBorder)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = BrandAccent)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${client.videosPerWeek} videos/semana · ${client.contentType.ifBlank { "General" }}",
                    color = BrandTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Client settings
            IconButton(onClick = onEditClientClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Configuración", tint = BrandTextSecondary)
            }
        }

        // Optional Preference Banner
        if (client.notes.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurfaceVariant.copy(alpha = 0.5f))
                    .border(width = 1.dp, color = BrandBorder)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = client.notes,
                    color = BrandTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        // Metrics Grid
        val doneCount = videos.count { it.status == "terminado" }
        val inProcessCount = videos.count { it.status == "en-proceso" }
        val revisionCount = videos.count { it.status == "revision" }
        val pendingCount = videos.count { it.status == "pendiente" }
        val totalCount = videos.size

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadgeCard(value = "$doneCount", label = "Terminados", tintColor = ColorTerminado)
            MetricBadgeCard(value = "$inProcessCount", label = "En proceso", tintColor = ColorEnProceso)
            MetricBadgeCard(value = "$revisionCount", label = "En revisión", tintColor = ColorRevision)
            MetricBadgeCard(value = "$pendingCount", label = "Pendientes", tintColor = ColorPendiente)
            MetricBadgeCard(value = "$totalCount", label = "Total", tintColor = BrandTextPrimary)
        }

        // Progress bar indicator
        val progressPercent = if (totalCount > 0) (doneCount * 100) / totalCount else 0
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandSurface)
                .border(width = 1.dp, color = BrandBorder, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedWeek?.label ?: "Semana",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "$progressPercent% completado",
                    color = BrandAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) doneCount.toFloat() / totalCount else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BrandAccent,
                trackColor = BrandSurfaceVariantDense
            )
        }

        // Weekly navigation scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(weeks) { wk ->
                    val isActive = wk.id == selectedWeek?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) BrandAccent else Color.Transparent)
                            .border(width = 1.dp, color = if (isActive) BrandAccent else BrandBorder, shape = RoundedCornerShape(20.dp))
                            .clickable { onWeekSelected(wk) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = wk.label,
                            color = if (isActive) BrandBg else BrandTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                item {
                    // New Week Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = BrandBorderLight,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onAddWeekClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Nueva semana",
                            color = BrandAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Videos Grid Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin videos o semanas guardadas todavía.",
                        color = BrandTextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(videos) { index, video ->
                        VideoGridItem(
                            index = index,
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        // Quick Share Bar
        if (selectedWeek != null && videos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface)
                    .border(width = 0.5.dp, color = BrandBorder)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onShareClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366), // Solid WhatsApp Green
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share tracker info summary",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compartir reporte con cliente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// INDIVIDUAL STATUS METRIC CHIPS
// ──────────────────────────────────────────────────────
@Composable
fun MetricBadgeCard(
    value: String,
    label: String,
    tintColor: Color
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandSurfaceVariant,
            contentColor = BrandTextPrimary
        ),
        border = BorderStroke(1.dp, BrandBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                color = tintColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                lineHeight = 18.sp
            )
            Text(
                text = label,
                color = BrandTextMuted,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// VIDEO CARD GRID ITEM
// ──────────────────────────────────────────────────────
@Composable
fun VideoGridItem(
    index: Int,
    video: Video,
    onClick: () -> Unit
) {
    val statusColor = when (video.status) {
        "terminado" -> ColorTerminado
        "en-proceso" -> ColorEnProceso
        "revision" -> ColorRevision
        else -> ColorPendiente
    }
    val statusText = when (video.status) {
        "terminado" -> "Terminado"
        "en-proceso" -> "En proceso"
        "revision" -> "En revisión"
        else -> "Pendiente"
    }
    
    val cardBorderColor = when (video.status) {
        "terminado" -> ColorTerminado.copy(alpha = 0.35f)
        "en-proceso" -> ColorEnProceso.copy(alpha = 0.3f)
        "revision" -> ColorRevision.copy(alpha = 0.3f)
        else -> BrandBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = BrandSurfaceVariant,
            contentColor = BrandTextPrimary
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Count index
            Text(
                text = String.format("%02d", index + 1),
                color = BrandTextMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = video.title.ifBlank { "Video ${index + 1}" },
                color = BrandTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Note bottom block if present
            if (video.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.note,
                    color = BrandTextSecondary,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// DIALOG: CLIENT CREATE / EDIT
// ──────────────────────────────────────────────────────
@Composable
fun ClientConfigDialog(
    title: String,
    initialName: String = "",
    initialVideos: Int = 10,
    initialType: String = "",
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onSave: (String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var videosRow by remember { mutableStateOf(initialVideos.toString()) }
    var contentType by remember { mutableStateOf(initialType) }
    var notes by remember { mutableStateOf(initialNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BrandSurface,
                contentColor = BrandTextPrimary
            ),
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandTextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Name field
                Text("Nombre del cliente *", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ej: Oscar Santos", color = BrandTextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrandSurfaceVariant,
                        unfocusedContainerColor = BrandSurfaceVariant,
                        focusedTextColor = BrandTextPrimary,
                        unfocusedTextColor = BrandTextPrimary,
                        focusedIndicatorColor = BrandAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Row for Videos & Type
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Videos por semana *", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = videosRow,
                            onValueChange = { videosRow = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = BrandSurfaceVariant,
                                unfocusedContainerColor = BrandSurfaceVariant,
                                focusedTextColor = BrandTextPrimary,
                                unfocusedTextColor = BrandTextPrimary,
                                focusedIndicatorColor = BrandAccent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Tipo de contenido", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = contentType,
                            onValueChange = { contentType = it },
                            placeholder = { Text("Reels, YouTube...", color = BrandTextMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = BrandSurfaceVariant,
                                unfocusedContainerColor = BrandSurfaceVariant,
                                focusedTextColor = BrandTextPrimary,
                                unfocusedTextColor = BrandTextPrimary,
                                focusedIndicatorColor = BrandAccent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes field
                Text("Notas / Preferencias", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Preferencias estéticas, música, etc...", color = BrandTextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrandSurfaceVariant,
                        unfocusedContainerColor = BrandSurfaceVariant,
                        focusedTextColor = BrandTextPrimary,
                        unfocusedTextColor = BrandTextPrimary,
                        focusedIndicatorColor = BrandAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemove != null) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Eliminar", color = ColorRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = BrandTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, videosRow.toIntOrNull() ?: 10, contentType, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAccent,
                            contentColor = BrandBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// DIALOG: VIDEO DETAILS EDITOR
// ──────────────────────────────────────────────────────
@Composable
fun VideoDetailsDialog(
    video: Video,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(video.title) }
    var status by remember { mutableStateOf(video.status) }
    var note by remember { mutableStateOf(video.note) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BrandSurface,
                contentColor = BrandTextPrimary
            ),
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Editor de Video",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandTextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text("Título del video", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ej: Vlog Semanal, Transición B-Roll", color = BrandTextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrandSurfaceVariant,
                        unfocusedContainerColor = BrandSurfaceVariant,
                        focusedTextColor = BrandTextPrimary,
                        unfocusedTextColor = BrandTextPrimary,
                        focusedIndicatorColor = BrandAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Choices
                Text("Estado", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val statuses = listOf(
                    "pendiente" to "⬜ Pendiente",
                    "en-proceso" to "🟡 En proceso",
                    "revision" to "🔵 En revisión",
                    "terminado" to "🟢 Terminado"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    statuses.forEach { (key, label) ->
                        val isSelected = status == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandSurfaceVariantDense else Color.Transparent)
                                .clickable { status = key }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { status = key },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandAccent,
                                    unselectedColor = BrandTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = if (isSelected) BrandTextPrimary else BrandTextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini notes
                Text("Nota / Ajustes pendientes", fontSize = 11.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ej: Falta agregar subtítulos...", color = BrandTextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrandSurfaceVariant,
                        unfocusedContainerColor = BrandSurfaceVariant,
                        focusedTextColor = BrandTextPrimary,
                        unfocusedTextColor = BrandTextPrimary,
                        focusedIndicatorColor = BrandAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = BrandTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(title, status, note)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAccent,
                            contentColor = BrandBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// DIALOG: REPORT SHARE & WHATSAPP
// ──────────────────────────────────────────────────────
@Composable
fun ShareTrackerSummaryDialog(
    client: Client,
    week: Week,
    videos: List<Video>,
    onDismiss: () -> Unit,
    context: Context
) {
    val summaryText = remember(client, week, videos) {
        formatWhatsAppMessage(client, week, videos)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BrandSurface,
                contentColor = BrandTextPrimary
            ),
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Compartir Reporte",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Envía un reporte detallado en tiempo real con emojis descriptivos a tu cliente vía WhatsApp.",
                    fontSize = 11.sp,
                    color = BrandTextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Plain Text Block Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBg)
                        .border(1.dp, BrandBorder, RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = summaryText,
                        color = BrandTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp Send Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(summaryText))
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir WhatsApp.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Copy to Clipboard Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Tracker Report", summaryText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Excelente! Reporte copiado en el portapapeles.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Copiar Reporte de Texto", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = BrandTextSecondary)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// VIEW STATE: EMPTY STATE SCREEN BACKUP
// ──────────────────────────────────────────────────────
@Composable
fun NoClientSelectedState(
    hasClients: Boolean,
    onAddClientClick: () -> Unit,
    onLoadSamplesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "🎬",
                fontSize = 54.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "Matias Films Manager",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = BrandTextPrimary,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Selecciona un cliente de la lista de la izquierda o crea uno nuevo para empezar a agendar, editar y reportar sus videos por semana.",
                color = BrandTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.widthIn(max = 280.dp)
            )
            
            if (!hasClients) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddClientClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAccent,
                        contentColor = BrandBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Agregar primer cliente", fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onLoadSamplesClick) {
                    Text("Cargar demostración de prueba", color = BrandAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// UTILS
// ══════════════════════════════════════════════════════
fun getInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "?"
    val parts = clean.split(" ")
    return if (parts.size >= 2) {
        (parts[0].take(1) + parts[1].take(1)).uppercase()
    } else {
        clean.take(2).uppercase()
    }
}

fun getAvatarColor(index: Int): Color {
    val items = AvatarColors
    return items.getOrElse(index) { items.first() }
}
