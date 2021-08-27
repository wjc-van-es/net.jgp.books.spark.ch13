package net.jgp.books.spark.ch13.lab110_flatten_shipment;

import static org.apache.spark.sql.functions.explode;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Processing of invoices formatted using the schema.org format.
 *
 * @author jgp
 */
public class FlattenShipmentDisplayApp {

    /**
     * main() is your entry point to the application.
     *
     * @param args
     */
    public static void main(String[] args) {
        FlattenShipmentDisplayApp app = new FlattenShipmentDisplayApp();
        app.start();
    }

    /**
     * The processing code.
     */
    private void start() {
        // Creates a session on a local master
        SparkSession spark = SparkSession.builder()
                .appName("Flatenning JSON doc describing shipments")
                .master("local")
                .getOrCreate();

        // Reads a JSON, stores it in a dataframe
        Dataset<Row> df = spark.read()
                .format("json")
                .option("multiline", true)
                .load("data/json/shipment.json");

        df = df
                // splitting the supplier struct (object) into a separate column per field
                .withColumn("supplier_name", df.col("supplier.name"))
                .withColumn("supplier_city", df.col("supplier.city"))
                .withColumn("supplier_state", df.col("supplier.state"))
                .withColumn("supplier_country", df.col("supplier.country"))
                .drop("supplier")

                // splitting the customer struct (object) into a separate column per field
                .withColumn("customer_name", df.col("customer.name"))
                .withColumn("customer_city", df.col("customer.city"))
                .withColumn("customer_state", df.col("customer.state"))
                .withColumn("customer_country", df.col("customer.country"))
                .drop("customer")

                // explode the array each item is a new row coupled with the same data from the other columns
                // hence we get a lot of duplication in the other columns and exploding is a denormalization of data
                .withColumn("items", explode(df.col("books")))
                .drop("books");
        // Shows at most 5 rows from the dataframe (there's only one anyway)
        df.show(5, false);
        df.printSchema();

        //continue splitting the items struct into separate qty and title columns
        df = df
                .withColumn("qty", df.col("items.qty"))
                .withColumn("title", df.col("items.title"))
                .drop("items");

        // Shows at most 5 rows from the dataframe (there's only one anyway)
        df.show(5, false);
        df.printSchema();

        df.createOrReplaceTempView("shipment_detail");
        Dataset<Row> bookCountDf =
                spark.sql("SELECT COUNT(*) AS bookCount FROM shipment_detail");
        bookCountDf.show(false);

        Dataset<Row> totalAmountDf =
                spark.sql("SELECT SUM(qty) AS totalAmount FROM shipment_detail");
        totalAmountDf.show(false); //expect 2 + 25 + 1 = 28
    }
}
