<%@ page contentType="text/html; charset=utf-8" language="java"
     import="org.ecocean.*,
java.util.Collection,
java.io.IOException,
java.util.ArrayList,
javax.jdo.Query,
java.util.List,
java.util.Map,
org.json.JSONObject,

org.ecocean.servlet.ServletUtilities,
org.ecocean.media.*
              "

%>
<html><head><title>Encounters with trivial Annotations</title>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script>
function update(data, callback) {
return;
    $.ajax({
        url: 'annotCheck.jsp?update',
        contentType: 'application/json',
        data: JSON.stringify(data),
        complete: function(d) {
            console.info('complete -> %o', d);
            if (typeof callback == 'function') callback(d.responseJSON);
        },
        dataType: 'json',
        type: 'POST'
    });
}


</script>
<style>
body {
    font-family: sans, arial;
}

.passed {
    opacity: 0.2;
}

div.ann {
    margin: 4px;
    display: inline-block;
    position: relative;
}
div.ann:hover {
    outline: solid 3px blue;
    opacity: 1.0;
}

div.ann img {
    max-height: 200px;
    max-width: 200px;
    min-width: 150px;
    min-height: 150px;
}

div.matchagainst-false {
    outline: solid 3px red;
    opacity: 0.3;
}

.caption {
    position: absolute;
    bottom: 2px;
    left: 0;
    width: 100%;
    background-color: rgba(255,255,255,0.5);
    font-size: 0.8em;
    color: black;
}

.caption a {
    text-decoration: none;
    color: black;
    cursor: pointer;
}
.caption a:hover {
    background-color: white;
}

.small {
    font-size: 0.8em;
    border-radius: 4px;
    padding: 2px 6px;
    background-color: 888;
}
</style>
</head>

<body><%
String context = ServletUtilities.getContext(request);
Shepherd myShepherd = new Shepherd(context);

/*
    String sql = "select" +
        "    \"ANNOTATION\".\"ID\" as annotId," +
        "    \"ANNOTATION\".\"ACMID\" as annotAcmId," +
        "    \"ENCOUNTER\".\"CATALOGNUMBER\" as encId," +
        "    \"ENCOUNTER\".\"INDIVIDUALID\" as indivId" +
        "from" +
        "    \"ANNOTATION\" join \"ENCOUNTER_ANNOTATIONS\" on (\"ENCOUNTER_ANNOTATIONS\".\"ID_EID\" = \"ANNOTATION\".\"ID\")" +
        "    join \"ENCOUNTER\" on (\"ENCOUNTER_ANNOTATIONS\".\"CATALOGNUMBER_OID\" = \"ENCOUNTER\".\"CATALOGNUMBER\")" +
        "where" +
        "    \"ANNOTATION\".\"ACMID\" IS NULL;"
*/

    //String sql = "SELECT org.ecocean.Annotation WHERE this.acmId == null && enc.annotations.contains(this) VARIABLES org.ecocean.Encounter enc";
    String sql = "SELECT FROM org.ecocean.Encounter WHERE this.annotations.contains(ann) && ann.acmId == null VARIABLES org.ecocean.Annotation ann";
    Query query = myShepherd.getPM().newQuery(sql);
    Collection c = (Collection)query.execute();
    ArrayList<Encounter> encs = new ArrayList<Encounter>(c);
    query.closeAll();

    for (Encounter enc : encs) {
        for (Annotation ann : enc.getAnnotations()) {
            if (ann.getAcmId() != null) continue;
%><div class="ann matchagainst-<%=ann.getMatchAgainst()%>">
    <img src="<%=ann.getMediaAsset().safeURL()%>" />
    <div class="caption">
        <a target="_new" href="../obrowse.jsp?type=Annotation&id=<%=ann.getId()%>"><%=ann.getId().substring(0,8)%></a>
    </div>
</div><%
        }
    }
%>

</body></html>
