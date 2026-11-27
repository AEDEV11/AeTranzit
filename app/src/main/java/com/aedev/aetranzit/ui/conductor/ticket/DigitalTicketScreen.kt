package com.aedev.aetranzit.ui.conductor.ticket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.usecase.TicketPayloadFormatter
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.BlueContainerLight
import com.aedev.aetranzit.ui.theme.BlueOnContainerLight
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.LightOutlineVariant
import com.aedev.aetranzit.ui.theme.NavySubtleText
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TealContainerLight
import com.aedev.aetranzit.ui.theme.TealOnContainerLight
import com.aedev.aetranzit.ui.theme.TicketPaperBackground
import com.aedev.aetranzit.ui.theme.TicketPaperDivider
import com.aedev.aetranzit.ui.theme.TicketPaperInk
import com.aedev.aetranzit.ui.theme.TicketPaperSectionBg
import com.aedev.aetranzit.ui.theme.TicketPaperSubtleInk
import com.aedev.aetranzit.ui.theme.TransitTeal
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DigitalTicketScreenEntryPoint {
    fun qrCodeGenerator(): QrCodeGenerator
    fun ticketShareHelper(): TicketShareHelper
}

@Composable
fun DigitalTicketScreen(
    onNavigateBack: () -> Unit,
    viewModel: DigitalTicketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, DigitalTicketScreenEntryPoint::class.java)
    }
    val qrCodeGenerator = remember(entryPoint) { entryPoint.qrCodeGenerator() }
    val ticketShareHelper = remember(entryPoint) { entryPoint.ticketShareHelper() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val qrImageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = uiState.qrPayload
    ) {
        val payload = uiState.qrPayload
        value = if (payload.isBlank()) {
            null
        } else {
            runCatching {
                qrCodeGenerator.generateQrBitmap(payload = payload).asImageBitmap()
            }.getOrNull()
        }
    }

    val handleEvent: (DigitalTicketUiEvent) -> Unit = { event ->
        when (event) {
            DigitalTicketUiEvent.ShareImageRequested -> {
                val renderData = uiState.toRenderData()
                if (!uiState.isSharingImage && renderData != null) {
                    viewModel.onEvent(DigitalTicketUiEvent.ShareImageStarted)
                    coroutineScope.launch {
                        val result = ticketShareHelper.shareTicketImage(
                            context = context,
                            renderData = renderData
                        )
                        val failureMsg = result.exceptionOrNull()?.message
                        viewModel.onEvent(DigitalTicketUiEvent.ShareImageCompleted(failureMsg))
                        if (failureMsg != null) {
                            snackbarHostState.showSnackbar(failureMsg)
                        }
                    }
                }
            }
            DigitalTicketUiEvent.SendSmsRequested -> {
                val result = ticketShareHelper.launchSmsReceiptIntent(
                    context = context,
                    smsBody = uiState.smsReceiptText
                )
                val failureMsg = result.exceptionOrNull()?.message
                if (failureMsg != null) {
                    viewModel.onEvent(DigitalTicketUiEvent.ActionFailed(failureMsg))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(failureMsg)
                    }
                }
            }
            else -> viewModel.onEvent(event)
        }
    }

    DigitalTicketContent(
        uiState = uiState,
        qrImageBitmap = qrImageBitmap,
        snackbarHostState = snackbarHostState,
        onEvent = handleEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalTicketContent(
    uiState: DigitalTicketUiState,
    qrImageBitmap: ImageBitmap?,
    onEvent: (DigitalTicketUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Digital Ticket",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val ticket = uiState.ticket
                        if (ticket != null) {
                            Text(
                                text = "Ref #${ticket.referenceCode} • Seat ${ticket.seatNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.sizeIn(
                            minWidth = AeDimens.MinTouchTarget,
                            minHeight = AeDimens.MinTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Seat Map"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.ticket != null) {
                DigitalTicketActionBar(
                    isSharingImage = uiState.isSharingImage,
                    onShareTicketImage = {
                        onEvent(DigitalTicketUiEvent.ShareImageRequested)
                    },
                    onSendSmsReceipt = {
                        onEvent(DigitalTicketUiEvent.SendSmsRequested)
                    }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.ticket == null -> {
                MissingTicketState(
                    message = uiState.errorMessage ?: "Ticket could not be found.",
                    onBackToSeatMap = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                val originStop = remember(uiState.route, uiState.routeLabel) {
                    uiState.route?.origin?.takeIf { it.isNotBlank() }
                        ?: uiState.routeLabel.substringBefore("→").trim().ifBlank { uiState.routeLabel }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = AeDimens.SpaceMd,
                            vertical = AeDimens.SpaceSmPlus
                        ),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
                ) {
                    val errorMsg = uiState.errorMessage
                    if (!errorMsg.isNullOrBlank()) {
                        ActionErrorBanner(
                            message = errorMsg,
                            onDismiss = { onEvent(DigitalTicketUiEvent.ClearError) }
                        )
                    }

                    PhotoReadyTicketCard(
                        companyName = uiState.companyName,
                        ticket = uiState.ticket,
                        originStop = originStop,
                        busNumber = uiState.busNumber,
                        plateNumber = uiState.plateNumber,
                        routeLabel = uiState.routeLabel,
                        conductorName = uiState.conductorName,
                        issuedTimestamp = uiState.issuedTimestamp,
                        notchCutoutColor = MaterialTheme.colorScheme.background,
                        qrImageBitmap = qrImageBitmap
                    )

                    Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
                }
            }
        }
    }
}

@Composable
private fun PhotoReadyTicketCard(
    companyName: String,
    ticket: Ticket,
    originStop: String,
    busNumber: String,
    plateNumber: String,
    routeLabel: String,
    conductorName: String,
    issuedTimestamp: String,
    notchCutoutColor: Color,
    qrImageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val isDroppedOff = ticket.status == TicketStatus.DROPPED_OFF
    val resolvedCompanyName = companyName.trim().ifBlank { Company.FALLBACK_BRAND_NAME }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = TicketPaperBackground,
            contentColor = TicketPaperInk
        ),
        border = BorderStroke(AeDimens.BorderWidth, LightOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.ElevatedCardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // High-Contrast Deep Navy Ticket Header with Bus Emblem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy)
                    .padding(
                        horizontal = AeDimens.SpaceMd,
                        vertical = AeDimens.SpaceMd
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_brand_bus_emblem),
                        contentDescription = "AeTranzit Bus Emblem",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(AeDimens.HeaderEmblemSize)
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSmPlus))
                    Column {
                        Text(
                            text = resolvedCompanyName,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Official Bus Passenger Receipt",
                            style = MaterialTheme.typography.bodySmall,
                            color = NavySubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AeDimens.SpaceSm))

                TicketStatusBadge(isDroppedOff = isDroppedOff)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
            ) {
                // Departure -> Destination Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                        Column {
                            Text(
                                text = "Departure",
                                style = MaterialTheme.typography.labelSmall,
                                color = TicketPaperSubtleInk
                            )
                            Text(
                                text = originStop,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TicketPaperInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TicketPaperSubtleInk,
                        modifier = Modifier
                            .padding(horizontal = AeDimens.SpaceXs)
                            .size(AeDimens.IconSm)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Drop-off Stop",
                                style = MaterialTheme.typography.labelSmall,
                                color = TicketPaperSubtleInk
                            )
                            Text(
                                text = ticket.destination,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TicketPaperInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TransitTeal,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                    }
                }

                // Full-Width Electric Blue Hero SEAT Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Seat ${ticket.seatNumber}, Reference ${ticket.referenceCode}"
                        },
                    shape = MaterialTheme.shapes.large,
                    color = ElectricBlue,
                    contentColor = PureWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AeDimens.SpaceMd,
                                vertical = AeDimens.SpaceSmPlus
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(min = AeDimens.TicketSeatBadgeMinWidth)
                        ) {
                            Text(
                                text = "SEAT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = ticket.seatNumber,
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.displaySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                                ),
                                color = PureWhite
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(AeDimens.TicketSeatDividerHeight),
                            thickness = AeDimens.BorderWidth,
                            color = PureWhite.copy(alpha = 0.35f)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXxs)
                        ) {
                            Text(
                                text = "Ref #${ticket.referenceCode}",
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                ),
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bus $busNumber • $plateNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = routeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Centered Passenger / Discount Pill
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (ticket.passengerType.isDiscounted) {
                        Surface(
                            shape = CircleShape,
                            color = TealContainerLight,
                            contentColor = TealOnContainerLight,
                            border = BorderStroke(AeDimens.BorderWidth, TransitTeal)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = AeDimens.SpaceMd,
                                    vertical = AeDimens.SpaceXs
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = TealOnContainerLight,
                                    modifier = Modifier.size(AeDimens.IconXs)
                                )
                                Text(
                                    text = "-20% ${ticket.passengerType.displayName} Discount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = TicketPaperSectionBg,
                            contentColor = TicketPaperInk,
                            border = BorderStroke(AeDimens.BorderWidth, TicketPaperDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = AeDimens.SpaceMd,
                                    vertical = AeDimens.SpaceXs
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(AeDimens.IconXs)
                                )
                                Text(
                                    text = "Passenger: ${ticket.passengerType.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Fare / Cash / Change Tabular Rows
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = TicketPaperSectionBg,
                    border = BorderStroke(AeDimens.BorderWidth, TicketPaperDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeDimens.SpaceSmPlus),
                        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        TicketAmountRow(
                            icon = Icons.Default.Payments,
                            iconTint = ElectricBlue,
                            label = "Fare Paid",
                            amount = formatPhp(ticket.fareAmount),
                            emphasize = true
                        )
                        HorizontalDivider(color = TicketPaperDivider)
                        TicketAmountRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = TransitTeal,
                            label = "Cash Tendered",
                            amount = formatPhp(ticket.cashTendered),
                            emphasize = false
                        )
                        TicketAmountRow(
                            icon = Icons.Default.CurrencyExchange,
                            iconTint = ElectricBlue,
                            label = "Change Given",
                            amount = formatPhp(ticket.changeAmount),
                            emphasize = false
                        )
                    }
                }

                // Compact Conductor & Timestamp Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conductor: $conductorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = TicketPaperSubtleInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                    Text(
                        text = issuedTimestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = TicketPaperSubtleInk,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Perforated Tear-Line with Side Cutout Notches
            PerforatedTicketDivider(notchCutoutColor = notchCutoutColor)

            // Pure-White High-Contrast QR Surface (Guaranteed Scannable in Light & Dark Mode)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AeDimens.SpaceMd,
                        end = AeDimens.SpaceMd,
                        top = AeDimens.SpaceSm,
                        bottom = AeDimens.SpaceMd
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = TicketPaperBackground,
                    border = BorderStroke(AeDimens.BorderWidth, TicketPaperDivider)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(AeDimens.QrContainerPadding)
                            .size(AeDimens.QrCodeSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrImageBitmap != null) {
                            Image(
                                bitmap = qrImageBitmap,
                                contentDescription = "Verification QR code for ticket #${ticket.referenceCode}",
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(
                                color = ElectricBlue,
                                modifier = Modifier.size(AeDimens.IconLg)
                            )
                        }
                    }
                }

                Text(
                    text = "Scan QR to open passenger web receipt",
                    style = MaterialTheme.typography.bodySmall,
                    color = TicketPaperSubtleInk,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Powered by AeTranzit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TicketPaperSubtleInk,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TicketStatusBadge(isDroppedOff: Boolean) {
    val containerColor = if (isDroppedOff) BlueContainerLight else TransitTeal
    val contentColor = if (isDroppedOff) BlueOnContainerLight else DeepNavy
    val label = if (isDroppedOff) "DROPPED OFF" else "VALID"
    val icon = if (isDroppedOff) Icons.Default.DirectionsBus else Icons.Default.CheckCircle

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AeDimens.SpaceSmPlus,
                vertical = AeDimens.SpaceXs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AeDimens.IconXs)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TicketAmountRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    amount: String,
    emphasize: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(AeDimens.IconSm)
            )
            Text(
                text = label,
                style = if (emphasize) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasize) TicketPaperInk else TicketPaperSubtleInk
            )
        }
        Text(
            text = amount,
            style = TabularAmountStyle.copy(
                fontSize = if (emphasize) {
                    MaterialTheme.typography.headlineSmall.fontSize
                } else {
                    MaterialTheme.typography.titleMedium.fontSize
                },
                lineHeight = if (emphasize) {
                    MaterialTheme.typography.headlineSmall.lineHeight
                } else {
                    MaterialTheme.typography.titleMedium.lineHeight
                }
            ),
            color = if (emphasize) ElectricBlue else TicketPaperInk
        )
    }
}

@Composable
private fun PerforatedTicketDivider(notchCutoutColor: Color) {
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    val notchHeight = AeDimens.TicketNotchRadius * 2

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(notchHeight)
    ) {
        val centerY = size.height / 2f
        val notchRadiusPx = size.height / 2f
        drawLine(
            color = TicketPaperDivider,
            start = Offset(notchRadiusPx, centerY),
            end = Offset(size.width - notchRadiusPx, centerY),
            strokeWidth = AeDimens.BorderWidthThick.toPx(),
            pathEffect = dashEffect
        )
        drawCircle(
            color = notchCutoutColor,
            radius = notchRadiusPx,
            center = Offset(0f, centerY)
        )
        drawCircle(
            color = notchCutoutColor,
            radius = notchRadiusPx,
            center = Offset(size.width, centerY)
        )
    }
}

@Composable
private fun DigitalTicketActionBar(
    isSharingImage: Boolean,
    onShareTicketImage: () -> Unit,
    onSendSmsReceipt: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AeDimens.CardElevation,
        shadowElevation = AeDimens.ElevatedCardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = AeDimens.SpaceMd,
                    vertical = AeDimens.SpaceSmPlus
                ),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Button(
                onClick = onShareTicketImage,
                enabled = !isSharingImage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AeDimens.PrimaryButtonHeight),
                shape = CircleShape
            ) {
                if (isSharingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AeDimens.IconSm),
                        strokeWidth = AeDimens.BorderWidthThick,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                    Text(
                        text = "Preparing Ticket Image…",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(AeDimens.IconSm)
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                    Text(
                        text = "Share Ticket Image",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            OutlinedButton(
                onClick = onSendSmsReceipt,
                enabled = !isSharingImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AeDimens.MinTouchTarget),
                shape = CircleShape,
                border = BorderStroke(AeDimens.BorderWidthThick, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(AeDimens.IconSm)
                )
                Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                Text(
                    text = "Send SMS Receipt",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AeDimens.SpaceMd,
                    top = AeDimens.SpaceSm,
                    bottom = AeDimens.SpaceSm,
                    end = AeDimens.SpaceXs
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(AeDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.sizeIn(
                    minWidth = AeDimens.MinTouchTarget,
                    minHeight = AeDimens.MinTouchTarget
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error"
                )
            }
        }
    }
}

@Composable
private fun MissingTicketState(
    message: String,
    onBackToSeatMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(AeDimens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(AeDimens.IconLg)
        )
        Spacer(modifier = Modifier.height(AeDimens.SpaceSmPlus))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AeDimens.SpaceMd))
        OutlinedButton(
            onClick = onBackToSeatMap,
            modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget),
            shape = CircleShape
        ) {
            Text(text = "Back to Seat Map")
        }
    }
}

private fun buildPreviewDigitalTicketState(
    passengerType: PassengerType = PassengerType.STUDENT,
    status: TicketStatus = TicketStatus.ACTIVE
): DigitalTicketUiState {
    val formatter = TicketPayloadFormatter()
    val sampleTicket = Ticket(
        id = "a94c71ef-820b-4f12-b91e-001122334455",
        tripId = "trip-preview-1",
        busId = "bus-preview-1",
        conductorId = "cond-preview-1",
        seatNumber = "14",
        destination = "Dau Terminal",
        passengerType = passengerType,
        fareAmount = if (passengerType.isDiscounted) 128.0 else 160.0,
        cashTendered = 200.0,
        changeAmount = if (passengerType.isDiscounted) 72.0 else 40.0,
        status = status,
        createdAt = 1_764_144_900_000L,
        isSynced = true,
        updatedAt = 1_764_144_900_000L
    )
    val bus = Bus(
        id = "bus-preview-1",
        busNumber = "AE-204",
        plateNumber = "NAA 4821",
        totalSeats = 45,
        columns = 5,
        isSynced = true,
        updatedAt = 1_764_144_000_000L
    )
    val route = Route(
        id = "route-preview-1",
        origin = "Cubao",
        destination = "Baguio",
        baseFare = 450.0,
        stops = emptyList(),
        isSynced = true,
        updatedAt = 1_764_144_000_000L
    )
    val qrPayload = formatter.buildQrPayload(ticket = sampleTicket)
    val previewCompanyName = "Victory Liner"
    val smsText = formatter.buildSmsReceiptText(
        ticket = sampleTicket,
        companyName = previewCompanyName,
        busNumber = bus.busNumber,
        plateNumber = bus.plateNumber,
        routeLabel = route.displayName,
        conductorName = "Juan Dela Cruz"
    )
    return DigitalTicketUiState(
        ticketId = sampleTicket.id,
        isLoading = false,
        companyName = previewCompanyName,
        ticket = sampleTicket,
        bus = bus,
        route = route,
        busNumber = bus.busNumber,
        plateNumber = bus.plateNumber,
        routeLabel = route.displayName,
        conductorName = "Juan Dela Cruz",
        issuedTimestamp = formatter.formatTimestamp(sampleTicket.createdAt),
        qrPayload = qrPayload,
        smsReceiptText = smsText
    )
}

@Preview(name = "Digital Ticket - Light (Discounted)", showBackground = true)
@Composable
private fun DigitalTicketContentLightPreview() {
    val previewState = remember {
        buildPreviewDigitalTicketState(
            passengerType = PassengerType.STUDENT,
            status = TicketStatus.ACTIVE
        )
    }
    val qrBitmap = remember(previewState.qrPayload) {
        runCatching {
            QrCodeGenerator.renderBitmapSync(
                payload = previewState.qrPayload,
                sizePx = 384
            ).asImageBitmap()
        }.getOrNull()
    }
    AeTranzitTheme(darkTheme = false) {
        DigitalTicketContent(
            uiState = previewState,
            qrImageBitmap = qrBitmap,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Digital Ticket - Dark (QR Contrast)", showBackground = true)
@Composable
private fun DigitalTicketContentDarkPreview() {
    val previewState = remember {
        buildPreviewDigitalTicketState(
            passengerType = PassengerType.SENIOR,
            status = TicketStatus.DROPPED_OFF
        )
    }
    val qrBitmap = remember(previewState.qrPayload) {
        runCatching {
            QrCodeGenerator.renderBitmapSync(
                payload = previewState.qrPayload,
                sizePx = 384
            ).asImageBitmap()
        }.getOrNull()
    }
    AeTranzitTheme(darkTheme = true) {
        DigitalTicketContent(
            uiState = previewState,
            qrImageBitmap = qrBitmap,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
