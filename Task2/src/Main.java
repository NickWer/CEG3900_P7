import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.*;

public class Main {
    private class KVP<K,V> { //completely insane that java doesn't have this
        V value;
        K key;

        KVP(){}

        KVP(K key, V value){
            this.key = key;
            this.value = value;
        }
    }

    public String getCountry(File f){
        return f.getName().split("-")[0];
    }

    /**
     * I think this code thrashes the disk pretty hard, like all good code does.
     * I'm not sure why.
     *
     * Maybe my 5400RPM drive is to blame and this code is perfectly fine.
     * Who knows?
     * @param path - Path to the directory
     */
    private Main(String path) {
        File folder = new File(path);
        //noinspection ConstantConditions
        Arrays.stream(folder.listFiles())
                .filter(File::isFile)
                .collect( //map files to Pairs<CountryCode, List<CourseDataFile>>
                        Collectors.groupingBy(this::getCountry)
                )
                .entrySet().stream().map(CountryFilePair -> {
                    KVP<String, Double> result = new KVP<>();
                    result.key = CountryFilePair.getKey();

                    //Flat map the list of files into a list of lines
                    result.value = CountryFilePair.getValue().stream().flatMap(file -> {
                        try {
                            return Files.lines(file.toPath());
                        } catch (IOException e) {
                            return (new ArrayList<String>()).stream();
                        }
                    }).mapToDouble(line -> { //map each line to a double
                        return Float.parseFloat(line.split("\\s+")[1]);
                    })
                            .average().orElse(0D);//average all the grades in all the courses for this country

                    return result;
                }).forEach(pair -> {
            System.out.println(pair.key + ": " + pair.value.toString());
        });
    }

    public static void main(String[] args) {
        new Main(args[0]);
    }
}
