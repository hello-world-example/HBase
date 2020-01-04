package xyz.kail.demo.hbase.core.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kail on 2018/3/10.
 */
public class MapReduceUtil {

    public static String[] agrsIO(Class c, String[] args) throws IOException {

        String localFile = c.getResource("").toString()
                .replace("file:", "")
                .replace("target/classes/", "src/main/java/");

        if (args.length > 0) {
            return args;
        }


        String input = localFile + "input";
        String output = localFile + "output";

        FileUtils.deleteDirectory(new File(output));


        List<String> params = Arrays.asList(
                input, output
        );
        return params.toArray(new String[params.size()]);
    }

}
