package model;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author adam
 */
public class WorldPerceptionInterface {
    
    private World world;
    
    public WorldPerceptionInterface(World world){
        this.world = world;
    }
    
    public Map<RelativePos,Integer> percieve(RelativePos pos, ArrayList<RelativePos> sensors){
        return this.world.percieve(pos, sensors);
    }
}
