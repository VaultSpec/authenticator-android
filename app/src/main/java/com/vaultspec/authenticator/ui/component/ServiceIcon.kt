package com.vaultspec.authenticator.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Maps issuer keywords to their domain for favicon lookup
private val serviceDomains: Map<String, String> = mapOf(
    "google" to "google.com",
    "github" to "github.com",
    "microsoft" to "microsoft.com",
    "facebook" to "facebook.com",
    "meta" to "meta.com",
    "twitter" to "x.com",
    "x" to "x.com",
    "discord" to "discord.com",
    "steam" to "store.steampowered.com",
    "reddit" to "reddit.com",
    "twitch" to "twitch.tv",
    "apple" to "apple.com",
    "amazon" to "amazon.com",
    "netflix" to "netflix.com",
    "spotify" to "spotify.com",
    "linkedin" to "linkedin.com",
    "paypal" to "paypal.com",
    "stripe" to "stripe.com",
    "aws" to "aws.amazon.com",
    "slack" to "slack.com",
    "dropbox" to "dropbox.com",
    "cloudflare" to "cloudflare.com",
    "gitlab" to "gitlab.com",
    "bitbucket" to "bitbucket.org",
    "npm" to "npmjs.com",
    "digitalocean" to "digitalocean.com",
    "instagram" to "instagram.com",
    "whatsapp" to "whatsapp.com",
    "telegram" to "telegram.org",
    "signal" to "signal.org",
    "snapchat" to "snapchat.com",
    "tiktok" to "tiktok.com",
    "proton" to "proton.me",
    "protonmail" to "proton.me",
    "coinbase" to "coinbase.com",
    "binance" to "binance.com",
    "adobe" to "adobe.com",
    "yahoo" to "yahoo.com",
    "zoom" to "zoom.us",
    "notion" to "notion.so",
    "figma" to "figma.com",
    "epic" to "epicgames.com",
    "ubisoft" to "ubisoft.com",
    "ea" to "ea.com",
    "okta" to "okta.com",
    "kraken" to "kraken.com",
    "bitwarden" to "bitwarden.com",
    "1password" to "1password.com",
    "lastpass" to "lastpass.com",
    "namecheap" to "namecheap.com",
    "ovh" to "ovh.com",
    "hetzner" to "hetzner.com",
    "mega" to "mega.nz",
    "pinterest" to "pinterest.com",
    "tumblr" to "tumblr.com",
    "wordpress" to "wordpress.com",
    "shopify" to "shopify.com",
    "heroku" to "heroku.com",
    "vercel" to "vercel.com",
    "netlify" to "netlify.com",
    "docker" to "docker.com",
    "jetbrains" to "jetbrains.com",
    "atlassian" to "atlassian.com",
    "jira" to "atlassian.com",
    "trello" to "trello.com",
    "asana" to "asana.com",
    "twilio" to "twilio.com",
    "sendgrid" to "sendgrid.com",
    "mailchimp" to "mailchimp.com",
    "hubspot" to "hubspot.com",
    "salesforce" to "salesforce.com",
    "oracle" to "oracle.com",
    "ibm" to "ibm.com",
    "samsung" to "samsung.com",
    "sony" to "sony.com",
    "nintendo" to "nintendo.com",
    "roblox" to "roblox.com",
    "riot" to "riotgames.com",
    "blizzard" to "blizzard.com",
    "battle.net" to "blizzard.com",
    "activision" to "activision.com",
    "rockstar" to "rockstargames.com",
    "xbox" to "xbox.com",
    "playstation" to "playstation.com",
    "gog" to "gog.com",
    "humble" to "humblebundle.com",
    "itch" to "itch.io",
    "kickstarter" to "kickstarter.com",
    "patreon" to "patreon.com",
    "etsy" to "etsy.com",
    "ebay" to "ebay.com",
    "aliexpress" to "aliexpress.com",
    "alibaba" to "alibaba.com",
    "upwork" to "upwork.com",
    "fiverr" to "fiverr.com",
    "uber" to "uber.com",
    "lyft" to "lyft.com",
    "airbnb" to "airbnb.com",
    "booking" to "booking.com",
    "expedia" to "expedia.com",
    "robinhood" to "robinhood.com",
    "crypto.com" to "crypto.com",
    "gemini" to "gemini.com",
    "blockfi" to "blockfi.com",
    "opensea" to "opensea.io",
    "gitlab" to "gitlab.com",
    "bitfinex" to "bitfinex.com",
    "bybit" to "bybit.com",
    "kucoin" to "kucoin.com",
    "gate" to "gate.io",
    "cPanel" to "cpanel.net",
    "godaddy" to "godaddy.com",
    "porkbun" to "porkbun.com",
    "cloudways" to "cloudways.com",
    "linode" to "linode.com",
    "vultr" to "vultr.com",
    "scaleway" to "scaleway.com",
    "azure" to "azure.microsoft.com",
    "gcloud" to "cloud.google.com",
    "nubank" to "nubank.com.br",
    "wise" to "wise.com",
    "revolut" to "revolut.com",
    "n26" to "n26.com",
)

private val WORD_SPLIT = Regex("[^a-z0-9.]+")

/** Domain to fetch a favicon from when the bundled pack has no icon for [issuer]. */
private fun faviconDomain(issuer: String): String {
    val lower = issuer.lowercase().trim()
    val words = lower.split(WORD_SPLIT).filter { it.isNotEmpty() }
    words.firstOrNull { it.contains('.') && !it.startsWith('.') && !it.endsWith('.') }?.let { return it }
    serviceDomains.entries.firstOrNull { (key, _) ->
        key in words || (key.length >= 4 && lower.contains(key))
    }?.let { return it.value }
    return lower.replace(Regex("[^a-z0-9]"), "") + ".com"
}

private class BrandLookup(val icon: BrandIcon?)

/**
 * Service logo tile. Uses the bundled brand pack when it knows the issuer (crisp, offline),
 * otherwise a remote favicon, otherwise a colored initial.
 */
@Composable
fun ServiceIcon(
    issuer: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.26f)

    // null = lookup still running
    val lookup by produceState(
        initialValue = BrandIconPack.cached(issuer)?.let { BrandLookup(it) }
            ?: if (BrandIconPack.isKnownMiss(issuer)) BrandLookup(null) else null,
        issuer,
    ) {
        if (value == null) {
            value = BrandLookup(
                withContext(Dispatchers.IO) { BrandIconPack.find(context.applicationContext, issuer) }
            )
        }
    }

    val result = lookup
    val icon = result?.icon
    when {
        // Hold a neutral tile while the lookup runs rather than flashing the fallback
        result == null -> Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        icon != null -> BrandTile(icon, issuer, size, shape, modifier)

        // Favicons are small bitmaps: inset them on a white tile instead of upscaling
        else -> SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data("https://icons.duckduckgo.com/ip3/${faviconDomain(issuer)}.ico")
                .crossfade(true)
                .build(),
            contentDescription = issuer,
            loading = { LetterFallback(issuer, size) },
            error = { LetterFallback(issuer, size) },
            success = image@{
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .background(Color.White)
                ) {
                    this@image.SubcomposeAsyncImageContent(
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(size * 0.64f),
                    )
                }
            },
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    }
}

@Composable
private fun BrandTile(
    icon: BrandIcon,
    issuer: String,
    size: Dp,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val brandLuminance = icon.color.luminance()
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Near-black brands disappear on dark cards: flip them to a white tile with a brand-colored glyph
    val invert = darkTheme && brandLuminance < 0.04f
    val tileColor = if (invert) Color.White else icon.color
    val glyphColor = when {
        invert -> icon.color
        brandLuminance > 0.6f -> Color(0xFF1C1C1E)
        else -> Color.White
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tileColor)
            .semantics { contentDescription = issuer }
    ) {
        Canvas(modifier = Modifier.size(size * 0.56f)) {
            scale(this.size.width / BrandIconPack.VIEWPORT, pivot = Offset.Zero) {
                drawPath(icon.path, glyphColor)
            }
        }
    }
}

@Composable
private fun LetterFallback(issuer: String, size: Dp) {
    val bgColor = generateColorFromName(issuer)
    val letter = issuer.firstOrNull()?.uppercase() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(bgColor)
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun generateColorFromName(name: String): Color {
    val hash = name.lowercase().hashCode()
    val hue = ((hash and 0x7FFFFFFF) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.45f)
}
