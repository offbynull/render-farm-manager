package com.offbynull.rfm.host.model.partition;

import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectVarId;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.Validate;

public abstract class Partition {
    private final UnmodifiableMap<String, Object> specificationId;
    
    Partition(Map<String, Object> specificationId) {
        Validate.notNull(specificationId);
        Validate.noNullElements(specificationId.keySet());
        specificationId.entrySet().forEach(e -> {
            String k = e.getKey();
            Object v = e.getValue();
            
            Validate.isTrue(k != null && v != null);
            isCorrectVarId(k, v.getClass());
        });
        
        this.specificationId = (UnmodifiableMap<String, Object>) unmodifiableMap(new HashMap<>(specificationId));
    }

    public UnmodifiableMap<String, Object> getSpecificationId() {
        return specificationId;
    }
}
