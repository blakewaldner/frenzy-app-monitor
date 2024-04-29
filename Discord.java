import com.mrpowergamerbr.temmiewebhook.embed.*;
import com.mrpowergamerbr.temmiewebhook.*;
import java.util.*;

public class Discord
{
    private String color;
    private String footer;
    private String footerIcon;
    
    public Discord(final String color, final String footer, final String footerIcon) {
        this.color = color;
        this.footer = footer;
        this.footerIcon = footerIcon;
    }
    
    public void webHookMessage(final String hook, final String store, final String currency, final String title, final String link, final String pic, final String desc, final String storeLocation, final double price, final String eta, final String coords, final boolean global, final String website, final String storePic, final boolean pickup) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ThumbnailEmbed thumbnail = new ThumbnailEmbed();
        thumbnail.setUrl(pic);
        final FieldEmbed feStoreLocation = new FieldEmbed();
        feStoreLocation.setName("Store Location");
        feStoreLocation.setValue(storeLocation);
        feStoreLocation.setInline(true);
        final FieldEmbed fePrice = new FieldEmbed();
        fePrice.setName("Price");
        fePrice.setValue(String.valueOf(price) + " " + currency);
        fePrice.setInline(true);
        final FieldEmbed feEta = new FieldEmbed();
        feEta.setName("Start Time");
        feEta.setValue(eta);
        feEta.setInline(true);
        final FieldEmbed feCoords = new FieldEmbed();
        feCoords.setName("Spoof Coordinates");
        feCoords.setValue(coords);
        feCoords.setInline(true);
        final FieldEmbed feGlobal = new FieldEmbed();
        feGlobal.setName("Global Shipping");
        feGlobal.setValue(new StringBuilder().append(global).toString());
        feGlobal.setInline(true);
        final FieldEmbed feMethod = new FieldEmbed();
        feMethod.setName("Delivery Method");
        String method = "";
        if (pickup) {
            method = "Pickup";
        }
        else {
            method = "Shipping";
        }
        feMethod.setValue(method);
        feMethod.setInline(true);
        final DiscordEmbed de = DiscordEmbed.builder().author(AuthorEmbed.builder().name(store).icon_url(storePic).url(website).build()).title(title).description(desc).thumbnail(thumbnail).field(fePrice).field(feEta).field(feMethod).field(feGlobal).field(feStoreLocation).field(feCoords).url(link).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().content("").embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
    }
}
