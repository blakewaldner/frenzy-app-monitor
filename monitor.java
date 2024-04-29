import org.json.simple.*;
import java.io.*;
import org.json.simple.parser.*;
import java.util.*;
import java.net.*;
import org.jsoup.*;

public class Monitor implements Runnable
{
    private Proxy currentProxy;
    private ArrayList<String> proxies;
    private int currentProxyIdx;
    private HashMap<String, String[]> hookEmbeds;
    private ArrayList<String> hooks;
    private int delay;
    private int attempts;
    private ArrayList<Long> backlog;
    private final String STOCK_ENDPOINT = "https://frenzy.shopifyapps.com/api/flashsales";
    
    public Monitor() throws IOException, ParseException {
        ThreadLocalAuthenticator.setAsDefault();
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        final Scanner scanProx = new Scanner(new File("proxies.txt"));
        this.proxies = new ArrayList<String>();
        while (scanProx.hasNextLine()) {
            this.proxies.add(scanProx.nextLine());
        }
        scanProx.close();
        final JSONParser parser = new JSONParser();
        final FileReader configFile = new FileReader("config.txt");
        final JSONObject config = (JSONObject)parser.parse(configFile);
        this.delay = Integer.parseInt(config.get("delay"));
        final FileReader embedFile = new FileReader("embed.txt");
        final JSONObject embed = (JSONObject)parser.parse(embedFile);
        final int webhooksAmt = embed.size();
        this.hookEmbeds = new HashMap<String, String[]>();
        this.hooks = new ArrayList<String>();
        final String[][] embeds = new String[webhooksAmt][3];
        for (int x = 0; x < embed.size(); ++x) {
            final JSONArray embedArray = embed.get(new StringBuilder().append(x + 1).toString());
            embeds[x][0] = embedArray.get(0);
            embeds[x][1] = embedArray.get(1);
            embeds[x][2] = embedArray.get(2);
            this.hookEmbeds.put(embedArray.get(3), embeds[x]);
            this.hooks.add(embedArray.get(3));
        }
        System.out.println("\n" + this.proxies.size() + " proxies loaded");
        this.attempts = 0;
    }
    
    public void run() {
        this.backlog = new ArrayList<Long>();
        this.rotateProxy();
        final JSONParser parse = new JSONParser();
        JSONArray sales = null;
        final List<Long> initialIDS = new ArrayList<Long>();
        List<Long> saleIDS = new ArrayList<Long>();
        try {
            final JSONArray initialSales = ((JSONObject)parse.parse(this.getPage("https://frenzy.shopifyapps.com/api/flashsales").body())).get("flashsales");
            for (int x = 0; x < initialSales.size(); ++x) {
                initialIDS.add(initialSales.get(x).get("id"));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParseException ex) {}
        while (true) {
            this.rotateProxy();
            try {
                sales = ((JSONObject)parse.parse(this.getPage("https://frenzy.shopifyapps.com/api/flashsales").body())).get("flashsales");
                saleIDS = new ArrayList<Long>();
                for (int x2 = 0; x2 < sales.size(); ++x2) {
                    saleIDS.add(sales.get(x2).get("id"));
                }
                if (!initialIDS.equals(saleIDS)) {
                    final List<Long> newSaleIDS = new ArrayList<Long>();
                    newSaleIDS.addAll(saleIDS);
                    newSaleIDS.removeAll(initialIDS);
                    for (int x = 0; x < newSaleIDS.size(); ++x) {
                        final Long id = newSaleIDS.get(x);
                        final int index = saleIDS.indexOf(id);
                        if (!this.backlog.contains(id)) {
                            this.newSale(sales.get(index));
                            this.backlog.add(id);
                            initialIDS.removeAll(initialIDS);
                            initialIDS.addAll(saleIDS);
                            this.attempts = 0;
                            System.out.println("Posted");
                        }
                    }
                }
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
            try {
                Thread.sleep(this.delay);
            }
            catch (InterruptedException e3) {
                e3.printStackTrace();
            }
            ++this.attempts;
            System.out.println("Attempt " + this.attempts + " with proxy " + this.currentProxy + " .... waiting " + this.delay + "ms");
        }
    }
    
    public void newSale(final JSONObject sale) {
        final JSONArray coordinates = sale.get("dropzone");
        final boolean global = sale.get("dropzone").size() == 0;
        String coords = "";
        if (global) {
            coords = "N/A";
        }
        else {
            int coordSize = coordinates.size();
            if (coordSize > 3) {
                coordSize = 3;
            }
            for (int x = 0; x < coordSize; ++x) {
                coords = String.valueOf(coords) + coordinates.get(x).get("lat") + ", " + coordinates.get(x).get("lng") + "\n";
            }
        }
        final String store = sale.get("shop").get("name");
        final String currency = sale.get("shop").get("currency");
        final String title = sale.get("title");
        final String link = "https://frenzy.sale/" + sale.get("password");
        final String pic = sale.get("foreground_image").get("src");
        final String desc = sale.get("description");
        final String storeLocation = String.valueOf(sale.get("shop").get("city")) + ", " + sale.get("shop").get("province_code") + ", " + sale.get("shop").get("country_name");
        final double price = sale.get("price_range").get("min");
        final String eta = sale.get("started_at");
        final String website = sale.get("shop").get("website_url");
        final String storePic = sale.get("logo_image").get("src");
        final boolean pickup = sale.get("pickup");
        for (int x2 = 0; x2 < this.hooks.size(); ++x2) {
            this.postDiscord(this.hooks.get(x2), store, currency, title, link, pic, desc, storeLocation, price, eta, coords, global, website, storePic, pickup);
        }
    }
    
    public void rotateProxy() {
        final String selectedProxy = this.proxies.get(this.currentProxyIdx++ % this.proxies.size());
        final String[] proxyArr = selectedProxy.split(":");
        if (proxyArr.length == 2) {
            final String host = proxyArr[0];
            final int port = Integer.parseInt(proxyArr[1]);
            this.currentProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        }
        else {
            final String host = proxyArr[0];
            final int port = Integer.parseInt(proxyArr[1]);
            final String username = proxyArr[2];
            final String password = proxyArr[3];
            ThreadLocalAuthenticator.setProxyAuth(username, password);
            this.currentProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        }
    }
    
    public Connection.Response getPage(final String url) throws IOException {
        return Jsoup.connect(url).proxy(this.currentProxy).followRedirects(true).ignoreContentType(true).method(Connection.Method.GET).timeout(20000).execute();
    }
    
    public void postDiscord(final String hook, final String store, final String currency, final String title, final String link, final String pic, final String desc, final String storeLocation, final double price, final String eta, final String coordinates, final boolean global, final String website, final String storePic, final boolean pickup) {
        final String[] embeds = this.hookEmbeds.get(hook);
        final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
        d.webHookMessage(hook, store, currency, title, link, pic, desc, storeLocation, price, eta, coordinates, global, website, storePic, pickup);
    }
}
