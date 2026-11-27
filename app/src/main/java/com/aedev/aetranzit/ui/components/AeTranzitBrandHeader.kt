package com.aedev.aetranzit.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.aedev.aetranzit.R
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme

@Composable
fun AeTranzitBrandHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = AeDimens.AuthHeroLogoSize
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "AeTranzit Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(logoSize)
        )
        Spacer(modifier = Modifier.height(AeDimens.SpaceSmPlus))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AeBrandCompactBar(
    modifier: Modifier = Modifier,
    brandName: String = "AeTranzit"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "AeTranzit Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(AeDimens.IconLg)
        )
        Text(
            text = brandName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AeTranzitBrandHeaderPreview() {
    AeTranzitTheme {
        Surface {
            AeTranzitBrandHeader(
                title = "AeTranzit",
                subtitle = "Offline-First Bus Ticketing & Fleet Operations",
                modifier = Modifier.padding(AeDimens.SpaceMd)
            )
        }
    }
}
