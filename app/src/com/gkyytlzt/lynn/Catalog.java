package com.gkyytlzt.lynn;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class Catalog {
    public static final String BASE = "https://pub-42b720ec62c14ff5a8bc9cd1cc953eef.r2.dev";
    public static final String PREFIX = "高考英语听力真题/localsend文件/";

    public static class Entry {
        public final String id;
        public final String title;
        public final int year;
        public final String fileName;
        public final long bytes;

        Entry(String id, String title, int year, String fileName, long bytes) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.fileName = fileName;
            this.bytes = bytes;
        }

        public String url() {
            StringBuilder sb = new StringBuilder(BASE);
            for (String seg : (PREFIX + fileName).split("/")) {
                sb.append('/').append(enc(seg));
            }
            return sb.toString();
        }

        public String sizeMb() {
            return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
        }
    }

    public static final Entry[] ENTRIES = new Entry[]{
            new Entry("2026_qg1", "2026 全国一卷", 2026, "2026_全国一卷.tklib", 28163016L),
            new Entry("2026_qg2", "2026 全国二卷", 2026, "2026_全国二卷.tklib", 21849176L),
            new Entry("2025_qg1", "2025 全国一卷", 2025, "2025_全国一卷.tklib", 37045502L),
            new Entry("2025_qg2", "2025 全国二卷", 2025, "2025_全国二卷.tklib", 25120840L),
            new Entry("2025_zjgz", "2025 浙江、贵州卷", 2025, "2025_浙江贵州卷.tklib", 49874953L),
            new Entry("2024_xkb1", "2024 新课标一卷", 2024, "2024_新课标一卷.tklib", 23872100L),
            new Entry("2024_xkb2", "2024 新课标二卷", 2024, "2024_新课标二卷.tklib", 47123899L),
            new Entry("2024_qgj", "2024 全国甲卷", 2024, "2024_全国甲卷.tklib", 11547307L),
            new Entry("2023_xkb12", "2023 新课标一二卷", 2023, "2023_新课标一二卷.tklib", 38014746L),
            new Entry("2023_qgab", "2023 全国甲、乙卷", 2023, "2023_全国甲乙卷.tklib", 12331105L),
            new Entry("2022_xgk12", "2022 新高考一二卷", 2022, "2022_新高考一二卷.tklib", 26394509L),
            new Entry("2022_qgab", "2022 全国甲、乙卷", 2022, "2022_全国甲乙卷.tklib", 25889841L),
            new Entry("2021_xgk", "2021 新高考浙江卷", 2021, "2021_新高考浙江卷.tklib", 12465493L),
            new Entry("2021_qgab", "2021 全国甲、乙卷", 2021, "2021_全国甲乙卷.tklib", 12678080L),
            new Entry("2020_qg1", "2020 全国一卷", 2020, "2020_全国一卷.tklib", 11981825L),
            new Entry("2020_qg23", "2020 全国二、三卷", 2020, "2020_全国二三卷.tklib", 11948837L),
            new Entry("2019_qg1", "2019 全国一卷", 2019, "2019_全国一卷.tklib", 12225216L),
            new Entry("2019_qg23", "2019 全国二、三卷", 2019, "2019_全国二三卷.tklib", 12293528L),
            new Entry("2018_qg1", "2018 全国一卷", 2018, "2018_全国一卷.tklib", 24065723L),
            new Entry("2018_qg23", "2018 全国二、三卷", 2018, "2018_全国二三卷.tklib", 23211738L),
            new Entry("2017_qg1", "2017 全国一卷", 2017, "2017_全国一卷.tklib", 34513794L),
            new Entry("2017_qg23", "2017 全国二、三卷", 2017, "2017_全国二三卷.tklib", 16479144L),
            new Entry("2016_qg1", "2016 全国一卷", 2016, "2016_全国一卷.tklib", 23068034L),
            new Entry("2016_qg23", "2016 全国二、三卷", 2016, "2016_全国二三卷.tklib", 22431058L),
            new Entry("2015_qg", "2015 全国卷", 2015, "2015_全国卷.tklib", 18908027L),
            new Entry("2014_qg", "2014 全国卷", 2014, "2014_全国卷.tklib", 20874232L),
            new Entry("2013_qg", "2013 全国卷", 2013, "2013_全国卷.tklib", 24040392L),
            new Entry("2012_qg", "2012 全国卷", 2012, "2012_全国卷.tklib", 11139773L),
            new Entry("2011_qg", "2011 全国卷", 2011, "2011_全国卷.tklib", 23987276L),
    };

    public static Entry byId(String id) {
        if (id == null) return null;
        for (Entry e : ENTRIES) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
