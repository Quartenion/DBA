package model;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author adam
 */
public interface WorldPerceptionInterface {
    public Map<RelativePos,Integer> percieve(RelativePos pos, ArrayList<RelativePos> sensors);
}
