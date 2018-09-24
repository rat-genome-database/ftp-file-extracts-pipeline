package edu.mcw.rgd;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 8/19/13
 * Time: 2:55 PM
 */
public class PubmedIdsManager {

    // cache of all rgd ids mapped to list of pubmed ids
    private Map<Integer, Set<String>> map = null;

    public void loadCuratedPubmedIds(FtpFileExtractsDAO dao, int objectKey, int speciesTypeKey) throws Exception {
        map = dao.getCuratedPubmedIds(objectKey, speciesTypeKey);
    }

    public boolean isCuratedPubmedId(int rgdId, String pubmedAccId) {
        Set<String> curatedPubmedIds = map.get(rgdId);
        if( curatedPubmedIds==null )
            return false;
        return curatedPubmedIds.contains(pubmedAccId);
    }

    public Set<String> getCuratedPubmedIds(int rgdId) {
        Set<String> curatedPubmedIds = map.get(rgdId);
        if( curatedPubmedIds==null )
            curatedPubmedIds = Collections.emptySet();
        return curatedPubmedIds;
    }

}
