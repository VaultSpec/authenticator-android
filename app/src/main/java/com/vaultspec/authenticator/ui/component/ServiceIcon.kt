package com.vaultspec.authenticator.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

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

@Composable
fun ServiceIcon(
    issuer: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val lowerIssuer = issuer.lowercase().trim()
    val domain = serviceDomains.entries.firstOrNull { lowerIssuer.contains(it.key) }?.value

    if (domain != null) {
        val faviconUrl = "https://icons.duckduckgo.com/ip3/$domain.ico"
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(faviconUrl)
                .crossfade(true)
                .build(),
            contentDescription = issuer,
            contentScale = ContentScale.Crop,
            loading = { LetterFallback(issuer, size) },
            error = { LetterFallback(issuer, size) },
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        // Try favicon from issuer as domain directly
        val guessDomain = lowerIssuer.replace(" ", "") + ".com"
        val faviconUrl = "https://icons.duckduckgo.com/ip3/$guessDomain.ico"
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(faviconUrl)
                .crossfade(true)
                .build(),
            contentDescription = issuer,
            contentScale = ContentScale.Crop,
            loading = { LetterFallback(issuer, size) },
            error = { LetterFallback(issuer, size) },
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
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
            .clip(CircleShape)
            .background(bgColor)
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun generateColorFromName(name: String): Color {
    val hash = name.lowercase().hashCode()
    val hue = ((hash and 0x7FFFFFFF) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.45f)
}
