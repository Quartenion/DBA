package model;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author adam
 */
public interface WorldPerceptionInterface {
    public Map<RelativePos,Integer> percieve(RelativePos pos, ArrayList<RelativePos> sensors);
}
