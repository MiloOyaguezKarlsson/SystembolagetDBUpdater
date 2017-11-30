package SystembolagetDBUpdater;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
    public static void main(String[] args) {
        try {
            UploadStores();
            System.out.println("Stores Uploaded");
            UploadArticles();
            System.out.println("Articles Uploaded");
            UploadStoreArticles();
            System.out.println("Store-Articles uploaded");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean UploadStores() throws ParserConfigurationException, IOException, SAXException, SQLException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL("https://www.systembolaget.se/api/assortment/stores/xml").openStream());

        Connection connection = ConnectionFactory.getConnection("jdbc:mysql://localhost/systembolagetdb");
        Statement stmt = (Statement) connection.createStatement();

        stmt.executeUpdate("TRUNCATE Stores"); //rensa hela tabellen för att sedan kunna fylla på utan fel med unika värden

        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("ButikOmbud");

        for (int i = 0; i < nList.getLength(); i++) {
            Node currentNode = nList.item(i);
            if (currentNode.getChildNodes().item(0).getTextContent().equals("Butik")) {
                int id = Integer.parseInt(currentNode.getChildNodes().item(1).getTextContent());
                String streetAddress = currentNode.getChildNodes().item(3).getTextContent();
                String city = currentNode.getChildNodes().item(6).getTextContent();
                city = city.toLowerCase();
                city = city.substring(0, 1).toUpperCase() + city.substring(1);
                String postalCode = currentNode.getChildNodes().item(5).getTextContent();
                if (postalCode.contains("S")) { // ta bort S- i post-nummret som ibland finns
                    postalCode = postalCode.substring(2);
                }

                String sql = String.format("INSERT INTO Stores VALUES (%d, '%s', '%s', '%s');", id, streetAddress, city, postalCode);

                stmt.executeUpdate(sql);
            }

        }
        connection.close();
        return true;
    }

    public static boolean UploadArticles() throws ParserConfigurationException, IOException, SAXException, SQLException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL("https://www.systembolaget.se/api/assortment/products/xml").openStream());

        Connection connection = ConnectionFactory.getConnection("jdbc:mysql://localhost/systembolagetdb");
        Statement stmt = (Statement) connection.createStatement();

        stmt.executeUpdate("TRUNCATE Articles");

        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("artikel");

        for (int i = 0; i < nList.getLength(); i++) {
            Node currentNode = nList.item(i);
            int artNr = Integer.parseInt(currentNode.getChildNodes().item(0).getTextContent());
            String name = currentNode.getChildNodes().item(3).getTextContent() + " " + currentNode.getChildNodes().item(4).getTextContent();
            name = name.replace("'", "");
            String group = currentNode.getChildNodes().item(10).getTextContent();
            if (!currentNode.getChildNodes().item(10).getNodeName().equals("Varugrupp")) {
                group = currentNode.getChildNodes().item(11).getTextContent();
            }
            String priceStr = currentNode.getChildNodes().item(5).getTextContent();
            String volumeStr = currentNode.getChildNodes().item(6).getTextContent();

            if (!currentNode.getChildNodes().item(6).getNodeName().equals("Volymiml")) {
                volumeStr = currentNode.getChildNodes().item(7).getTextContent();
            }

            int volume = Integer.parseInt(volumeStr.substring(0, volumeStr.indexOf(".")));
            int price = Integer.parseInt(priceStr.substring(0, priceStr.indexOf(".")));

            String sql = String.format("INSERT INTO Articles VALUES (%d, '%s', '%s', %d, %d);", artNr, name, group, price, volume);
            stmt.executeUpdate(sql);

        }
        connection.close();
        return true;
    }

    public static boolean UploadStoreArticles() throws ParserConfigurationException, IOException, SAXException, SQLException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL("https://www.systembolaget.se/api/assortment/stock/xml").openStream());

        Connection connection = ConnectionFactory.getConnection("jdbc:mysql://localhost/systembolagetdb");
        Statement stmt = (Statement) connection.createStatement();

        stmt.executeUpdate("TRUNCATE storearticles");

        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("Butik");

        for (int i = 0; i < nList.getLength(); i++) {
            Node currentNode = nList.item(i);
            int storeID = Integer.parseInt(currentNode.getAttributes().getNamedItem("ButikNr").getNodeValue());
            NodeList currentNList = currentNode.getChildNodes();
            for (int j = 0; j < currentNList.getLength(); j++) {
                int articleID = Integer.parseInt(currentNList.item(j).getTextContent());
                String sql = String.format("INSERT INTO storearticles VALUES(%d, %d)", storeID, articleID);
                stmt.executeUpdate(sql);
            }
        }
        connection.close();
        return true;
    }

}
