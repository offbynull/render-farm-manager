package com.offbynull.rfm.host.testutils;

import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class TestUtils {
    private TestUtils() {
        // do nothing
    }
    
    public static Specification loadSpecResource(String name) throws ClassNotFoundException, IOException {
        try (InputStream is = TestUtils.class.getResourceAsStream(name)) {
            String str = IOUtils.toString(is, StandardCharsets.UTF_8);
            return loadSpecString(str);
        }
    }
    
    public static Specification loadSpecString(String str) throws ClassNotFoundException, IOException {
        return drill(null, new StringReader(str));
    }
    
    private static String drillId(Reader reader) throws IOException {
        String id = "";
        int ch;
        reader.mark(1);
        while ((ch = reader.read()) != -1) {
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || (ch == '_')
                    || (ch == '-')
                    || (ch == '/')) {
                id = id + (char) ch;
                reader.mark(1);
            } else {
                reader.reset();
                return id;
            }
        }
        
        return id;
    }

    private static Object convertValue(String name, String value) throws IOException {
        switch (name.substring(0, 2)) {
            case "b_":
                return Boolean.valueOf(value);
            case "n_":
                return new BigDecimal(value);
            case "s_":
                return value;
            default:
                throw new IllegalArgumentException();
        }
    }
    
    private static Specification drill(String name, Reader reader) throws IOException {
        Map<String, Object> props = new HashMap<>();
        List<Specification> specs = new ArrayList<>();
        
        String id = "";
        reader.mark(1);
                
        int ch;
        top:
        while ((ch = reader.read()) != -1) {
            switch (ch) {
                case '{':
                    Specification spec = drill(id, reader);
                    specs.add(spec);
                    break;
                case '}':
                    break top;
                case ':':
                    String valueStr = drillId(reader);
                    Object value = convertValue(id, valueStr);
                    props.put(id, value);
                    break;
                case ' ':
                case '\r':
                case '\n':
                    break;
                default:
                    reader.reset();
                    id = drillId(reader);
                    Validate.isTrue(!id.isEmpty());
                    break;
            }
            
            reader.mark(1);
        }
        
        if (name == null) {
            return specs.get(0);
        }
        
        switch (name) {
            case "host":
                return new HostSpecification(
                        specs.stream().filter(x -> x instanceof SocketSpecification).map(x -> (SocketSpecification) x).collect(toList()),
                        specs.stream().filter(x -> x instanceof GpuSpecification).map(x -> (GpuSpecification) x).collect(toList()),
                        specs.stream().filter(x -> x instanceof MountSpecification).map(x -> (MountSpecification) x).collect(toList()),
                        specs.stream().filter(x -> x instanceof RamSpecification).map(x -> (RamSpecification) x).collect(toList()),
                        props);
            case "socket":
                return new SocketSpecification(
                        specs.stream().filter(x -> x instanceof CoreSpecification).map(x -> (CoreSpecification) x).collect(toList()),
                        props);
            case "core":
                return new CoreSpecification(
                        specs.stream().filter(x -> x instanceof CpuSpecification).map(x -> (CpuSpecification) x).collect(toList()),
                        props);
            case "cpu":
                return new CpuSpecification(props);
            case "gpu":
                return new GpuSpecification(props);
            case "mount":
                return new MountSpecification(props);
            case "ram":
                return new RamSpecification(props);
            default:
                throw new IllegalArgumentException();
        }
    }
}
