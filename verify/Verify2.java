import java.io.*; import java.nio.file.*; import java.time.*; import java.time.format.*;
import java.util.*; import java.util.regex.*; import java.util.stream.*;

// Mirrors the RESTRUCTURED ConsensusService.detect() exactly, to confirm the
// transcription reproduces the verified counts (AC=8, electrician=4, plumber=3).
public class Verify2 {
  record Msg(int id, LocalDateTime ts, String sender, String text) {}
  record RM(int id, String sender, String ts, String text, double score) {}
  static final Pattern HEADER = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2}), (\\d{2}:\\d{2}) - (.*)$");
  static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
  static List<Msg> parse(List<String> lines){List<Msg> out=new ArrayList<>();int id=0;StringBuilder b=null;LocalDateTime t=null;String s=null;
    for(String raw:lines){Matcher m=HEADER.matcher(raw);if(m.matches()){if(b!=null&&s!=null)out.add(new Msg(id++,t,s,b.toString().strip()));b=null;s=null;t=null;
      LocalDateTime ts=LocalDateTime.parse(m.group(1)+" "+m.group(2),DTF);String rest=m.group(3);int c=rest.indexOf(": ");if(c<0)continue;String snd=rest.substring(0,c);if(snd.length()>40||snd.contains(":"))continue;s=snd;t=ts;b=new StringBuilder(rest.substring(c+2));}else if(b!=null)b.append('\n').append(raw);}
    if(b!=null&&s!=null)out.add(new Msg(id,t,s,b.toString().strip()));return out;}

  static final Set<String> STOP=new HashSet<>(Arrays.asList("the","and","for","are","was","this","that","very","good","call","yes","not","haan","also","did","does","who","what","when","where","how","pls","please","need","anyone","someone","from","with","said","says","recommend","recommends","recommendation","recommendations","reliable","honest","same","will","have","has","your","our","his","her","him","she","they","them","use","used","using","get","got","can","just","imo","btw","today","yesterday","last","month","summer","winter","time","number","contact","reasonable","charges","rate","fair","done","fine","nice","great","society","greenwood","wing","floor","pinned","list","give","gives","adding","added","repair","service","servicing","serviced","technician","mechanic","guy","wala","bhai","aircon","cooling","cool","care","refill","split","unit","units","wiring","wireman","electrician","electrical","switch","switchboard","switchboards","regulator","regulators","socket","spark","sparking","plumber","plumbing","pipeline","drainage","leak","leakage","sink","blockage","choked","bathroom","fitting","carpenter","painter","painting","noise","work","stopped","working"));
  static final Pattern WORD=Pattern.compile("[A-Za-z]+");
  static List<String> cand(String text){List<String> r=new ArrayList<>();Matcher m=WORD.matcher(text.toLowerCase());while(m.find()){String t=m.group();if(t.length()<4)continue;if(STOP.contains(t))continue;r.add(t);}return r;}
  static String find(Map<String,String> p,String x){while(!p.get(x).equals(x)){p.put(x,p.get(p.get(x)));x=p.get(x);}return x;}
  static void union(Map<String,String> p,String a,String b){p.put(find(p,a),find(p,b));}

  record Cluster(String label,int n,List<String> senders,List<Integer> msgs){}
  static List<Cluster> detect(List<RM> retrieved,int minSenders){
    Map<String,Set<String>> tokSenders=new HashMap<>();Map<String,Set<Integer>> tokMsgs=new HashMap<>();
    for(RM msg:retrieved)for(String t:new HashSet<>(cand(msg.text()))){tokSenders.computeIfAbsent(t,k->new HashSet<>()).add(msg.sender());tokMsgs.computeIfAbsent(t,k->new HashSet<>()).add(msg.id());}
    List<String> keys=new ArrayList<>();for(var e:tokSenders.entrySet())if(e.getValue().size()>=minSenders)keys.add(e.getKey());
    Map<String,String> parent=new HashMap<>();for(String k:keys)parent.put(k,k);
    for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++){String a=keys.get(i),b=keys.get(j);Set<Integer> in=new HashSet<>(tokMsgs.get(a));in.retainAll(tokMsgs.get(b));if(in.size()>=2)union(parent,a,b);}
    Map<String,Set<String>> gS=new HashMap<>();Map<String,Set<Integer>> gM=new HashMap<>();Map<String,String> gL=new HashMap<>();Map<String,Integer> gB=new HashMap<>();
    for(String k:keys){String root=find(parent,k);gS.computeIfAbsent(root,r->new HashSet<>()).addAll(tokSenders.get(k));gM.computeIfAbsent(root,r->new HashSet<>()).addAll(tokMsgs.get(k));int sc=tokSenders.get(k).size();if(sc>gB.getOrDefault(root,-1)){gB.put(root,sc);gL.put(root,k);}}
    List<Cluster> out=new ArrayList<>();for(String root:gS.keySet()){Set<String> snd=gS.get(root);if(snd.size()<minSenders)continue;List<String> sl=new ArrayList<>(snd);sl.sort(String::compareTo);List<Integer> ml=new ArrayList<>(gM.get(root));ml.sort(Integer::compareTo);out.add(new Cluster(gL.get(root),snd.size(),sl,ml));}
    out.sort((x,y)->Integer.compare(y.n(),x.n()));return out;}

  static List<RM> kw(List<Msg> all,String...any){return all.stream().filter(m->{String l=m.text().toLowerCase();for(String k:any)if(l.contains(k))return true;return false;}).map(m->new RM(m.id(),m.sender(),m.ts().toString(),m.text(),1.0)).collect(Collectors.toList());}

  public static void main(String[] a) throws Exception {
    List<Msg> all=parse(Files.readAllLines(Path.of(a[0])));
    String[][] qs={{"AC repair","ac "," ac","aircon","cooling","cool care","ramesh"},{"electrician","electric","wiring","socket","switch","sunil","wireman"},{"plumber","plumb","leak","tap","sink","drainage","iqbal","pipeline"}};
    for(String[] q:qs){List<RM> r=kw(all,Arrays.copyOfRange(q,1,q.length));List<Cluster> cs=detect(r,2);
      System.out.println("Query: "+q[0]+"  (retrieved "+r.size()+")");
      for(Cluster c:cs)System.out.println("   -> '"+c.label()+"' x"+c.n()+" "+c.senders());
    }
  }
}
