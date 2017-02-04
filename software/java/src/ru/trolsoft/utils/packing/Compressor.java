package ru.trolsoft.utils.packing;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created on 31/01/17.
 */
public class Compressor {
    private int offset;
    private int length;
    private int history;
    private int refSize;
    private int m;
    private int maxLength;

    class Key {
        final int val;

        Key(int[] data, int offset, int size) {
            int v = 0;
            for (int i = 0; i < size; i++) {
                if (offset + i < data.length) {
                    v += data[offset + i] << (size - i - 1) * 8;
                }
            }
            val = v;
        }

        Key(int val) {
            this.val = val;
        }

        @Override
        public String toString() {
            char ch1 = (char) (((val >> 8)) & 0xff);
            char ch2 = (char)(val & 0xff);
            return ""+ch1 + ch2;
        }
    }

    class DataPacket {
        final int val;
        final int offset;

        DataPacket(int val) {
            this.val = val;
            this.offset = -1;
        }

        DataPacket(int offset, int val) {
            this.val = val;
            this.offset = offset;
        }
    }

    public Compressor(int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.history = 1 << offset;
        this.refSize = 1 + offset + length;
        if (refSize < 9) {
            m = 1;
        } else if (refSize < 18) {
            m = 2;
        } else {
            m = 3;
        }
        maxLength = m + (1 << length) - 1;
    }

    /**
     * Return the length of the common prefix of s1 and s2
     * @param s1
     * @param s2
     * @return
     */
    private int prefix(int[] s1, int[] s2) {
        for (int i = 0; i < s2.length; i++) {
            if (s1[i % s1.length] != s2[i]) {
                return i;
            }
        }
        return s2.length;
    }

static int ppp;
    public int[] compress(int[] src) {
        List<DataPacket> sched = new ArrayList<>();
        Map<Integer, Set<Integer>> lempel = new HashMap<>();
        int pos = 0;
        while (pos < src.length) {
ppp=pos;
log("pos", ""+pos);
            Key k = new Key(src, pos, m);
            int older = pos - history - 1;
            Set<Integer> candidates = lempel.get(k.val);
            if (candidates == null) {
                candidates = new HashSet<>();
            }
            log("k", "'" + k + "' " + older);
//            System.out.println("K='" + k + "' older=" + older);
Map<Key, Set<Integer>> lll = new HashMap<>();
for (int kk : lempel.keySet()) {
    lll.put(new Key(kk), lempel.get(kk));
}
            log("l","" + lll);
            log("c","" + asSortedList(candidates));
            int bestLen = 0;
            int bestPos = 0;
            for (int p : candidates) {
                int s1[] = Arrays.copyOfRange(src, p, pos);
                int s2[] = Arrays.copyOfRange(src, pos, src.length-1);
                int pr = prefix(s1, s2);

                if (pr > bestLen) {
                    bestLen = pr;
                    bestPos = p;
                }
                if (pos == 170) {
                    System.out.println(": " + pr + " " + p + "  " + candidates);
                    //System.out.println(":" + Arrays.toString(s1) + " : " + Arrays.toString(s2));
                }

            }
            log("best", "" + bestLen + " " + bestPos);
            if (lempel.containsKey(k.val)) {
                lempel.get(k.val).add(pos);
            } else {
                lempel.put(k.val, new HashSet<>());
                lempel.get(k.val).add(pos);
            }
            bestLen = Math.min(bestLen, maxLength);
            if (bestLen >= m) {
                sched.add(new DataPacket(bestPos - pos, bestLen));
                pos += bestLen;
            } else {
                sched.add(new DataPacket(src[pos]));
                pos++;
            }

//            K=in  older=-230
//            L= {' c': set([23]), 'co': set([24]), '#!': set([0]), '# ': set([18]), '!/': set([1]), 'in': set([8]), 'py': set([11]), '/b': set([6]), 'th': set([13]), '/u': set([2]), '\n#': set([17]), 'n\n': set([16]), 'di': set([26]), 'bi': set([7]), ' -': set([19]), '*-': set([21]), 'ho': set([14]), 'yt': set([12]), 'on': set([15]), '- ': set([22]), 'n/': set([9]), 'sr': set([4]), 'od': set([25]), 'us': set([3]), '-*': set([20]), '/p': set([10]), 'r/': set([5])}
//            C-> [8]

//            K=):  older=66
//            L= {'__': set([316, 310]), ' c': set([23]), 'gt': set([121]), '(s': set([89, 318, 198, 175]), ' i': set([187, 189]), ': ': set([30]), ' o': set([145, 124]), ' l': set([171, 117, 221]), ' s': set([155, 93]), ' p': set([82, 138]), ' a': set([151]), '(o': set([292]), ' t': set([48, 113]), ' u': set([31]), ' B': set([282]), 'y\n': set([76]), ':\n': set([97, 239]), 'la': set([278]), 'tu': set([109, 255]), 'tr': set([287]), 'Bi': set([283]), ' R': set([106]), 'th': set([128, 122, 13]), 'ti': set([49]), 'tf': set([33]), '2)': set([177, 95]), ' "': set([158]), '  ': set([208, 249, 99, 100, 245]), ' !': set([230]), 'di': set([26]), 'de': set([306, 79]), 'g:': set([29]), ' -': set([19, 37]), 'd ': set([154]), 'ys': set([62]), 'yt': set([12]), ' =': set([169]), '- ': set([22]), 'Re': set([107]), '-*': set([20]), 't)': set([298]), '/p': set([10]), '-8': set([35]), 't ': set([47]), 'el': set([320]), 'en': set([119]), '#!': set([0]), '# ': set([18]), 'ec': set([296]), 'et': set([108]), 'z)': set([200]), 'rt': set([46]), 'rr': set([73]), 're': set([288, 266, 84, 253]), 'ra': set([193, 74]), 'rn': set([111]), 'lf': set([321]), 'e(': set([197]), 'z ': set([168]), 'e ': set([130, 116]), 'bj': set([294]), 'bi': set([7]), 'le': set([118]), 'po': set([44]), 'je': set([295]), 'on': set([136, 15]), 'e\n': set([52]), 'of': set([125]), 'od': set([25]), 'ob': set([293]), ']:': set([238]), '] ': set([229]), 'r/': set([5]), 'or': set([185, 45]), '):': set([96, 201, 299]), 'co': set([24, 132]), 'cl': set([277]), '!/': set([1]), 'ts': set([285]), '!=': set([231]), 'ct': set([297]), 'pr': set([83]), 'py': set([11]), 'm(': set([291]), '[i': set([217, 236]), '8 ': set([36]), 'x(': set([88]), 'ef': set([80, 85]), '_i': set([311]), 'ho': set([14]), '% ': set([220]), 'he': set([115]), ')]': set([228]), 'mm': set([134]), 'ut': set([32]), 'mo': set([135]), 'us': set([3]), 'ur': set([110]), 'ss': set([280]), 'mp': set([43]), '1)': set([227]), '1,': set([91]), 'ix': set([87]), 'am': set([290]), 'it': set([314, 284]), 'an': set([152]), 'f ': set([81, 213, 126]), 's\n': set([63]), 'as': set([279]), 'ar': set([72]), 'im': set([42, 50]), '\n\n': set([275, 77]), 'in': set([8, 312, 27]), 'ay': set([75]), 'f)': set([322]), 'f-': set([34]), 'if': set([212]), 'ni': set([313]), ', ': set([92]), 's2': set([234, 94]), 's1': set([90, 149, 215]), 'nd': set([153]), '/b': set([6]), 'ng': set([120, 195]), '/u': set([2]), 's ': set([281]), '\n ': set([98, 179, 162, 261]), '\n#': set([17]), 'n\n': set([16]), '""': set([104, 159, 103]), '*-': set([21]), 'i ': set([218, 188]), 'fo': set([184]), 'sz': set([273, 167]), 'sy': set([61]), 'n/': set([9]), 'n ': set([112, 191]), 'sr': set([4]), 'st': set([286]), 'i\n': set([260]), '\ni': set([64, 41, 53]), '\nd': set([78]), '= ': set([232, 170])}
//            C-> [96, 201, 299]

        }
System.out.println("scheedsize = " + sched.size());
        return toArray(sched);
    }

    private int[] toArray(List<DataPacket> sched) {
        BitStream bs = new BitStream();
        bs.append(4, offset);
        bs.append(4, length);
        bs.append(2, m);
        bs.append(16, sched.size());
        for (DataPacket d : sched) {
            if (d.offset >= 0) {
                bs.append(1, 1);
                bs.append(offset, - d.offset - 1);
                bs.append(length, d.val - m);
            } else {
                bs.append(1, 0);
                bs.append(8, d.val);
            }
        }
        return bs.toArray();
    }

    private static int[] readFile(String path) {
        File f = new File(path);
        int[] result = new int[(int)f.length()];
        try (Reader r = new BufferedReader(new FileReader(f))) {
            for (int i = 0; i < result.length; i++) {
                result[i] = r.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void writeFile(String path, int[] data) {
        try (Writer w = new BufferedWriter(new FileWriter(path))) {
            for (int v : data) {
                w.write(v);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws FileNotFoundException {
        log("pos", null);
        log("k", null);
        log("l", null);
        log("c", null);
        log("best", null);
        Compressor c = new Compressor(8, 3);
//        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("/Users/trol/Projects/avr/avr-bootloader/software/java.log"))));
        int data[] = readFile("/Users/trol/Projects/avr/avr-bootloader/software/data.txt");
        writeFile("/Users/trol/Projects/avr/avr-bootloader/software/data.jpack", c.compress(data));

//        System.out.flush();
    }



    private  static void log(String name, String val) {
        name = "/Users/trol/Projects/avr/avr-bootloader/software/java-" + name + ".log";
        if (val == null) {
            new File(name).delete();
            try {
                new File(name).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            Files.write(Paths.get(name), (ppp+": "+val + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }


    /*

    def compress(self, blk):
        lempel = {}
        sched = []
        pos = 0
        while pos < len(blk):
            k = blk[pos:pos+self.M]
            older = (pos - self.history - 1)
            candidates = [p for p in lempel.get(k, []) if (older < p)]
            (bestlen, bestpos) = max([(0, 0)] + [(prefix(blk[p:pos], blk[pos:]), p) for p in candidates])
            if k in lempel:
                lempel[k].add(pos)
            else:
                lempel[k] = set([pos])
            bestlen = min(bestlen, self.maxlen)
            if bestlen >= self.M:
                sched.append((bestpos - pos, bestlen))
                pos += bestlen
            else:
                sched.append(blk[pos])
                pos += 1
        return sched

    def toarray(self, blk):
        bs = Bitstream()
        bs.append(4, self.b_off)
        bs.append(4, self.b_len)
        bs.append(2, self.M)
        sched = self.compress(blk)
        bs.append(16, len(sched))
        for c in sched:
            if len(c) != 1:
                (offset, l) = c
                bs.append(1, 1)
                bs.append(self.b_off, -offset - 1)
                bs.append(self.b_len, l - self.M)
            else:
                bs.append(1, 0)
                bs.append(8, ord(c))
        return bs.toarray()


    def to_cfile(self, hh, blk, name):
        print >>hh, "static PROGMEM prog_uchar %s[] = {" % name
        bb = self.toarray(blk)
        for i in range(0, len(bb), 16):
            if (i & 0xff) == 0:
                print >>hh
            for c in bb[i:i+16]:
                print >>hh, "0x%02x, " % c,
            print >>hh
        print >>hh, "};"

    def decompress(self, sched):
        s = ""
        for c in sched:
            if len(c) == 1:
                s += c
            else:
                (offset, l) = c
                for i in range(l):
                    s += s[offset]
        return s
     */
}
