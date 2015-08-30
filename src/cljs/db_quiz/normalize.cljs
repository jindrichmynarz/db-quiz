(ns db-quiz.normalize
  (:refer-clojure :exclude [replace])
  (:require [db-quiz.config :refer [config]]
            [clojure.string :refer [join lower-case replace split trim]]))

(defn abbreviate-tokens
  "Convert tokens into an abbreviation."
  [tokens]
  (apply str (map (fn [s] (if (zero? (.indexOf (lower-case s) "ch")) "Ch" (first s)))
                  tokens)))

(defn collapse-whitespace
  "Replace consecutive whitespace characters with a single space."
  [text]
  (replace text #"\s{2,}" " "))

(defn clear-tokens
  "Filter out roman numerals"
  [tokens]
  (filter (fn [token]
            (not (re-matches #"^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})\.?$" token)))
          tokens))

(defn delete-parenthesized-parts
  "Remove parts in parentheses or brackets from text."
  [text]
  (-> text
      (replace #"\s*\([^)]+\)" "")
      (replace #"\s*\[[^\]]+\]" "")))

(defn remove-punctuation
  "Remove punctuation from s.
  Regular expression taken from <http://stackoverflow.com/a/21224179/385505>."
  [s]
  (replace s #"[\-=_!\"#%&'*{},.\/:;?\(\)\[\]@\\$\^*+<>~`\u00a1\u00a7\u00b6\u00b7\u00bf\u037e\u0387\u055a-\u055f\u0589\u05c0\u05c3\u05c6\u05f3\u05f4\u0609\u060a\u060c\u060d\u061b\u061e\u061f\u066a-\u066d\u06d4\u0700-\u070d\u07f7-\u07f9\u0830-\u083e\u085e\u0964\u0965\u0970\u0af0\u0df4\u0e4f\u0e5a\u0e5b\u0f04-\u0f12\u0f14\u0f85\u0fd0-\u0fd4\u0fd9\u0fda\u104a-\u104f\u10fb\u1360-\u1368\u166d\u166e\u16eb-\u16ed\u1735\u1736\u17d4-\u17d6\u17d8-\u17da\u1800-\u1805\u1807-\u180a\u1944\u1945\u1a1e\u1a1f\u1aa0-\u1aa6\u1aa8-\u1aad\u1b5a-\u1b60\u1bfc-\u1bff\u1c3b-\u1c3f\u1c7e\u1c7f\u1cc0-\u1cc7\u1cd3\u2016\u2017\u2020-\u2027\u2030-\u2038\u203b-\u203e\u2041-\u2043\u2047-\u2051\u2053\u2055-\u205e\u2cf9-\u2cfc\u2cfe\u2cff\u2d70\u2e00\u2e01\u2e06-\u2e08\u2e0b\u2e0e-\u2e16\u2e18\u2e19\u2e1b\u2e1e\u2e1f\u2e2a-\u2e2e\u2e30-\u2e39\u3001-\u3003\u303d\u30fb\ua4fe\ua4ff\ua60d-\ua60f\ua673\ua67e\ua6f2-\ua6f7\ua874-\ua877\ua8ce\ua8cf\ua8f8-\ua8fa\ua92e\ua92f\ua95f\ua9c1-\ua9cd\ua9de\ua9df\uaa5c-\uaa5f\uaade\uaadf\uaaf0\uaaf1\uabeb\ufe10-\ufe16\ufe19\ufe30\ufe45\ufe46\ufe49-\ufe4c\ufe50-\ufe52\ufe54-\ufe57\ufe5f-\ufe61\ufe68\ufe6a\ufe6b\uff01-\uff03\uff05-\uff07\uff0a\uff0c\uff0e\uff0f\uff1a\uff1b\uff1f\uff20\uff3c\uff61\uff64\uff65]+" ""))

(def ^:private default-diacritics-removal-map
  "Clojurescript version of <http://stackoverflow.com/a/18391901/385505>"
  [
   {:base "A"
    :letters "\u0041\u24B6\uFF21\u00C0\u00C1\u00C2\u1EA6\u1EA4\u1EAA\u1EA8\u00C3\u0100\u0102\u1EB0\u1EAE\u1EB4\u1EB2\u0226\u01E0\u00C4\u01DE\u1EA2\u00C5\u01FA\u01CD\u0200\u0202\u1EA0\u1EAC\u1EB6\u1E00\u0104\u023A\u2C6F"}
   {:base "AA"
    :letters "\uA732"}
   {:base "AE"
    :letters "\u00C6\u01FC\u01E2"}
   {:base "AO"
    :letters "\uA734"}
   {:base "AU"
    :letters "\uA736"}
   {:base "AV"
    :letters "\uA738\uA73A"}
   {:base "AY"
    :letters "\uA73C"}
   {:base "B"
    :letters "\u0042\u24B7\uFF22\u1E02\u1E04\u1E06\u0243\u0182\u0181"}
   {:base "C"
    :letters "\u0043\u24B8\uFF23\u0106\u0108\u010A\u010C\u00C7\u1E08\u0187\u023B\uA73E"}
   {:base "D"
    :letters "\u0044\u24B9\uFF24\u1E0A\u010E\u1E0C\u1E10\u1E12\u1E0E\u0110\u018B\u018A\u0189\uA779"}
   {:base "DZ"
    :letters "\u01F1\u01C4"}
   {:base "Dz"
    :letters "\u01F2\u01C5"}
   {:base "E"
    :letters "\u0045\u24BA\uFF25\u00C8\u00C9\u00CA\u1EC0\u1EBE\u1EC4\u1EC2\u1EBC\u0112\u1E14\u1E16\u0114\u0116\u00CB\u1EBA\u011A\u0204\u0206\u1EB8\u1EC6\u0228\u1E1C\u0118\u1E18\u1E1A\u0190\u018E"}
   {:base "F"
    :letters "\u0046\u24BB\uFF26\u1E1E\u0191\uA77B"}
   {:base "G"
    :letters "\u0047\u24BC\uFF27\u01F4\u011C\u1E20\u011E\u0120\u01E6\u0122\u01E4\u0193\uA7A0\uA77D\uA77E"}
   {:base "H"
    :letters "\u0048\u24BD\uFF28\u0124\u1E22\u1E26\u021E\u1E24\u1E28\u1E2A\u0126\u2C67\u2C75\uA78D"}
   {:base "I"
    :letters "\u0049\u24BE\uFF29\u00CC\u00CD\u00CE\u0128\u012A\u012C\u0130\u00CF\u1E2E\u1EC8\u01CF\u0208\u020A\u1ECA\u012E\u1E2C\u0197"}
   {:base "J"
    :letters "\u004A\u24BF\uFF2A\u0134\u0248"}
   {:base "K"
    :letters "\u004B\u24C0\uFF2B\u1E30\u01E8\u1E32\u0136\u1E34\u0198\u2C69\uA740\uA742\uA744\uA7A2"}
   {:base "L"
    :letters "\u004C\u24C1\uFF2C\u013F\u0139\u013D\u1E36\u1E38\u013B\u1E3C\u1E3A\u0141\u023D\u2C62\u2C60\uA748\uA746\uA780"}
   {:base "LJ"
    :letters "\u01C7"}
   {:base "Lj"
    :letters "\u01C8"}
   {:base "M"
    :letters "\u004D\u24C2\uFF2D\u1E3E\u1E40\u1E42\u2C6E\u019C"}
   {:base "N"
    :letters "\u004E\u24C3\uFF2E\u01F8\u0143\u00D1\u1E44\u0147\u1E46\u0145\u1E4A\u1E48\u0220\u019D\uA790\uA7A4"}
   {:base "NJ"
    :letters "\u01CA"}
   {:base "Nj"
    :letters "\u01CB"}
   {:base "O"
    :letters "\u004F\u24C4\uFF2F\u00D2\u00D3\u00D4\u1ED2\u1ED0\u1ED6\u1ED4\u00D5\u1E4C\u022C\u1E4E\u014C\u1E50\u1E52\u014E\u022E\u0230\u00D6\u022A\u1ECE\u0150\u01D1\u020C\u020E\u01A0\u1EDC\u1EDA\u1EE0\u1EDE\u1EE2\u1ECC\u1ED8\u01EA\u01EC\u00D8\u01FE\u0186\u019F\uA74A\uA74C"}
   {:base "OI"
    :letters "\u01A2"}
   {:base "OO"
    :letters "\uA74E"}
   {:base "OU"
    :letters "\u0222"}
   {:base "OE"
    :letters "\u008C\u0152"}
   {:base "oe"
    :letters "\u009C\u0153"}
   {:base "P"
    :letters "\u0050\u24C5\uFF30\u1E54\u1E56\u01A4\u2C63\uA750\uA752\uA754"}
   {:base "Q"
    :letters "\u0051\u24C6\uFF31\uA756\uA758\u024A"}
   {:base "R"
    :letters "\u0052\u24C7\uFF32\u0154\u1E58\u0158\u0210\u0212\u1E5A\u1E5C\u0156\u1E5E\u024C\u2C64\uA75A\uA7A6\uA782"}
   {:base "S"
    :letters "\u0053\u24C8\uFF33\u1E9E\u015A\u1E64\u015C\u1E60\u0160\u1E66\u1E62\u1E68\u0218\u015E\u2C7E\uA7A8\uA784"}
   {:base "T"
    :letters "\u0054\u24C9\uFF34\u1E6A\u0164\u1E6C\u021A\u0162\u1E70\u1E6E\u0166\u01AC\u01AE\u023E\uA786"}
   {:base "TZ"
    :letters "\uA728"}
   {:base "U"
    :letters "\u0055\u24CA\uFF35\u00D9\u00DA\u00DB\u0168\u1E78\u016A\u1E7A\u016C\u00DC\u01DB\u01D7\u01D5\u01D9\u1EE6\u016E\u0170\u01D3\u0214\u0216\u01AF\u1EEA\u1EE8\u1EEE\u1EEC\u1EF0\u1EE4\u1E72\u0172\u1E76\u1E74\u0244"}
   {:base "V"
    :letters "\u0056\u24CB\uFF36\u1E7C\u1E7E\u01B2\uA75E\u0245"}
   {:base "VY"
    :letters "\uA760"}
   {:base "W"
    :letters "\u0057\u24CC\uFF37\u1E80\u1E82\u0174\u1E86\u1E84\u1E88\u2C72"}
   {:base "X"
    :letters "\u0058\u24CD\uFF38\u1E8A\u1E8C"}
   {:base "Y"
    :letters "\u0059\u24CE\uFF39\u1EF2\u00DD\u0176\u1EF8\u0232\u1E8E\u0178\u1EF6\u1EF4\u01B3\u024E\u1EFE"}
   {:base "Z"
    :letters "\u005A\u24CF\uFF3A\u0179\u1E90\u017B\u017D\u1E92\u1E94\u01B5\u0224\u2C7F\u2C6B\uA762"}
   {:base "a"
    :letters "\u0061\u24D0\uFF41\u1E9A\u00E0\u00E1\u00E2\u1EA7\u1EA5\u1EAB\u1EA9\u00E3\u0101\u0103\u1EB1\u1EAF\u1EB5\u1EB3\u0227\u01E1\u00E4\u01DF\u1EA3\u00E5\u01FB\u01CE\u0201\u0203\u1EA1\u1EAD\u1EB7\u1E01\u0105\u2C65\u0250"}
   {:base "aa"
    :letters "\uA733"}
   {:base "ae"
    :letters "\u00E6\u01FD\u01E3"}
   {:base "ao"
    :letters "\uA735"}
   {:base "au"
    :letters "\uA737"}
   {:base "av"
    :letters "\uA739\uA73B"}
   {:base "ay"
    :letters "\uA73D"}
   {:base "b"
    :letters "\u0062\u24D1\uFF42\u1E03\u1E05\u1E07\u0180\u0183\u0253"}
   {:base "c"
    :letters "\u0063\u24D2\uFF43\u0107\u0109\u010B\u010D\u00E7\u1E09\u0188\u023C\uA73F\u2184"}
   {:base "d"
    :letters "\u0064\u24D3\uFF44\u1E0B\u010F\u1E0D\u1E11\u1E13\u1E0F\u0111\u018C\u0256\u0257\uA77A"}
   {:base "dz"
    :letters "\u01F3\u01C6"}
   {:base "e"
    :letters "\u0065\u24D4\uFF45\u00E8\u00E9\u00EA\u1EC1\u1EBF\u1EC5\u1EC3\u1EBD\u0113\u1E15\u1E17\u0115\u0117\u00EB\u1EBB\u011B\u0205\u0207\u1EB9\u1EC7\u0229\u1E1D\u0119\u1E19\u1E1B\u0247\u025B\u01DD"}
   {:base "f"
    :letters "\u0066\u24D5\uFF46\u1E1F\u0192\uA77C"}
   {:base "g"
    :letters "\u0067\u24D6\uFF47\u01F5\u011D\u1E21\u011F\u0121\u01E7\u0123\u01E5\u0260\uA7A1\u1D79\uA77F"}
   {:base "h"
    :letters "\u0068\u24D7\uFF48\u0125\u1E23\u1E27\u021F\u1E25\u1E29\u1E2B\u1E96\u0127\u2C68\u2C76\u0265"}
   {:base "hv"
    :letters "\u0195"}
   {:base "i"
    :letters "\u0069\u24D8\uFF49\u00EC\u00ED\u00EE\u0129\u012B\u012D\u00EF\u1E2F\u1EC9\u01D0\u0209\u020B\u1ECB\u012F\u1E2D\u0268\u0131"}
   {:base "j"
    :letters "\u006A\u24D9\uFF4A\u0135\u01F0\u0249"}
   {:base "k"
    :letters "\u006B\u24DA\uFF4B\u1E31\u01E9\u1E33\u0137\u1E35\u0199\u2C6A\uA741\uA743\uA745\uA7A3"}
   {:base "l"
    :letters "\u006C\u24DB\uFF4C\u0140\u013A\u013E\u1E37\u1E39\u013C\u1E3D\u1E3B\u017F\u0142\u019A\u026B\u2C61\uA749\uA781\uA747"}
   {:base "lj"
    :letters "\u01C9"}
   {:base "m"
    :letters "\u006D\u24DC\uFF4D\u1E3F\u1E41\u1E43\u0271\u026F"}
   {:base "n"
    :letters "\u006E\u24DD\uFF4E\u01F9\u0144\u00F1\u1E45\u0148\u1E47\u0146\u1E4B\u1E49\u019E\u0272\u0149\uA791\uA7A5"}
   {:base "nj"
    :letters "\u01CC"}
   {:base "o"
    :letters "\u006F\u24DE\uFF4F\u00F2\u00F3\u00F4\u1ED3\u1ED1\u1ED7\u1ED5\u00F5\u1E4D\u022D\u1E4F\u014D\u1E51\u1E53\u014F\u022F\u0231\u00F6\u022B\u1ECF\u0151\u01D2\u020D\u020F\u01A1\u1EDD\u1EDB\u1EE1\u1EDF\u1EE3\u1ECD\u1ED9\u01EB\u01ED\u00F8\u01FF\u0254\uA74B\uA74D\u0275"}
   {:base "oi"
    :letters "\u01A3"}
   {:base "ou"
    :letters "\u0223"}
   {:base "oo"
    :letters "\uA74F"}
   {:base "p"
    :letters "\u0070\u24DF\uFF50\u1E55\u1E57\u01A5\u1D7D\uA751\uA753\uA755"}
   {:base "q"
    :letters "\u0071\u24E0\uFF51\u024B\uA757\uA759"}
   {:base "r"
    :letters "\u0072\u24E1\uFF52\u0155\u1E59\u0159\u0211\u0213\u1E5B\u1E5D\u0157\u1E5F\u024D\u027D\uA75B\uA7A7\uA783"}
   {:base "s"
    :letters "\u0073\u24E2\uFF53\u00DF\u015B\u1E65\u015D\u1E61\u0161\u1E67\u1E63\u1E69\u0219\u015F\u023F\uA7A9\uA785\u1E9B"}
   {:base "t"
    :letters "\u0074\u24E3\uFF54\u1E6B\u1E97\u0165\u1E6D\u021B\u0163\u1E71\u1E6F\u0167\u01AD\u0288\u2C66\uA787"}
   {:base "tz"
    :letters "\uA729"}
   {:base "u"
    :letters  "\u0075\u24E4\uFF55\u00F9\u00FA\u00FB\u0169\u1E79\u016B\u1E7B\u016D\u00FC\u01DC\u01D8\u01D6\u01DA\u1EE7\u016F\u0171\u01D4\u0215\u0217\u01B0\u1EEB\u1EE9\u1EEF\u1EED\u1EF1\u1EE5\u1E73\u0173\u1E77\u1E75\u0289"}
   {:base "v"
    :letters "\u0076\u24E5\uFF56\u1E7D\u1E7F\u028B\uA75F\u028C"}
   {:base "vy"
    :letters "\uA761"}
   {:base "w"
    :letters "\u0077\u24E6\uFF57\u1E81\u1E83\u0175\u1E87\u1E85\u1E98\u1E89\u2C73"}
   {:base "x"
    :letters "\u0078\u24E7\uFF58\u1E8B\u1E8D"}
   {:base "y"
    :letters "\u0079\u24E8\uFF59\u1EF3\u00FD\u0177\u1EF9\u0233\u1E8F\u00FF\u1EF7\u1E99\u1EF5\u01B4\u024F\u1EFF"}
   {:base "z"
    :letters "\u007A\u24E9\uFF5A\u017A\u1E91\u017C\u017E\u1E93\u1E95\u01B6\u0225\u0240\u2C6C\uA763"}
  ])

(def diacritics-map
  (->> default-diacritics-removal-map
       (map (fn [{:keys [base letters]}] (map #(vector % base) letters)))
       (apply concat)
       (into {})))

(defn replace-diacritics
  "Replace diacritical characters in s with their ASCII analogues."
  [s]
  (replace s
           #"[^\u0000-\u007E]"
           (fn [character] (or (diacritics-map character) character))))

(defn replace-surface-forms
  "Replace a set of surface-forms appearing in description with abbreviation."
  [description abbreviation surface-forms]
  (loop [[surface-form & the-rest] surface-forms
         result description]
    (let [clean-result (replace result surface-form abbreviation)]
      (if-not the-rest
        clean-result
        (recur the-rest clean-result)))))

(defn space-sentences
  "Put a space after sentence end."
  [s]
  (replace s #"\.(?=\u0041|\u0042|\u0043|\u0044|\u0045|\u0046|\u0047|\u0048|\u0049|\u004A|\u004B|\u004C|\u004D|\u004E|\u004F|\u0050|\u0051|\u0052|\u0053|\u0054|\u0055|\u0056|\u0057|\u0058|\u0059|\u005A|\u00C0|\u00C1|\u00C2|\u00C3|\u00C4|\u00C5|\u00C6|\u00C7|\u00C8|\u00C9|\u00CA|\u00CB|\u00CC|\u00CD|\u00CE|\u00CF|\u00D0|\u00D1|\u00D2|\u00D3|\u00D4|\u00D5|\u00D6|\u00D8|\u00D9|\u00DA|\u00DB|\u00DC|\u00DD|\u00DE|\u0100|\u0102|\u0104|\u0106|\u0108|\u010A|\u010C|\u010E|\u0110|\u0112|\u0114|\u0116|\u0118|\u011A|\u011C|\u011E|\u0120|\u0122|\u0124|\u0126|\u0128|\u012A|\u012C|\u012E|\u0130|\u0132|\u0134|\u0136|\u0139|\u013B|\u013D|\u013F|\u0141|\u0143|\u0145|\u0147|\u014A|\u014C|\u014E|\u0150|\u0152|\u0154|\u0156|\u0158|\u015A|\u015C|\u015E|\u0160|\u0162|\u0164|\u0166|\u0168|\u016A|\u016C|\u016E|\u0170|\u0172|\u0174|\u0176|\u0178|\u0179|\u017B|\u017D|\u0181|\u0182|\u0184|\u0186|\u0187|\u0189|\u018A|\u018B|\u018E|\u018F|\u0190|\u0191|\u0193|\u0194|\u0196|\u0197|\u0198|\u019C|\u019D|\u019F|\u01A0|\u01A2|\u01A4|\u01A6|\u01A7|\u01A9|\u01AC|\u01AE|\u01AF|\u01B1|\u01B2|\u01B3|\u01B5|\u01B7|\u01B8|\u01BC|\u01C4|\u01C7|\u01CA|\u01CD|\u01CF|\u01D1|\u01D3|\u01D5|\u01D7|\u01D9|\u01DB|\u01DE|\u01E0|\u01E2|\u01E4|\u01E6|\u01E8|\u01EA|\u01EC|\u01EE|\u01F1|\u01F4|\u01F6|\u01F7|\u01F8|\u01FA|\u01FC|\u01FE|\u0200|\u0202|\u0204|\u0206|\u0208|\u020A|\u020C|\u020E|\u0210|\u0212|\u0214|\u0216|\u0218|\u021A|\u021C|\u021E|\u0220|\u0222|\u0224|\u0226|\u0228|\u022A|\u022C|\u022E|\u0230|\u0232|\u023A|\u023B|\u023D|\u023E|\u0241|\u0243|\u0244|\u0245|\u0246|\u0248|\u024A|\u024C|\u024E|\u0370|\u0372|\u0376|\u0386|\u0388|\u0389|\u038A|\u038C|\u038E|\u038F|\u0391|\u0392|\u0393|\u0394|\u0395|\u0396|\u0397|\u0398|\u0399|\u039A|\u039B|\u039C|\u039D|\u039E|\u039F|\u03A0|\u03A1|\u03A3|\u03A4|\u03A5|\u03A6|\u03A7|\u03A8|\u03A9|\u03AA|\u03AB|\u03CF|\u03D2|\u03D3|\u03D4|\u03D8|\u03DA|\u03DC|\u03DE|\u03E0|\u03E2|\u03E4|\u03E6|\u03E8|\u03EA|\u03EC|\u03EE|\u03F4|\u03F7|\u03F9|\u03FA|\u03FD|\u03FE|\u03FF|\u0400|\u0401|\u0402|\u0403|\u0404|\u0405|\u0406|\u0407|\u0408|\u0409|\u040A|\u040B|\u040C|\u040D|\u040E|\u040F|\u0410|\u0411|\u0412|\u0413|\u0414|\u0415|\u0416|\u0417|\u0418|\u0419|\u041A|\u041B|\u041C|\u041D|\u041E|\u041F|\u0420|\u0421|\u0422|\u0423|\u0424|\u0425|\u0426|\u0427|\u0428|\u0429|\u042A|\u042B|\u042C|\u042D|\u042E|\u042F|\u0460|\u0462|\u0464|\u0466|\u0468|\u046A|\u046C|\u046E|\u0470|\u0472|\u0474|\u0476|\u0478|\u047A|\u047C|\u047E|\u0480|\u048A|\u048C|\u048E|\u0490|\u0492|\u0494|\u0496|\u0498|\u049A|\u049C|\u049E|\u04A0|\u04A2|\u04A4|\u04A6|\u04A8|\u04AA|\u04AC|\u04AE|\u04B0|\u04B2|\u04B4|\u04B6|\u04B8|\u04BA|\u04BC|\u04BE|\u04C0|\u04C1|\u04C3|\u04C5|\u04C7|\u04C9|\u04CB|\u04CD|\u04D0|\u04D2|\u04D4|\u04D6|\u04D8|\u04DA|\u04DC|\u04DE|\u04E0|\u04E2|\u04E4|\u04E6|\u04E8|\u04EA|\u04EC|\u04EE|\u04F0|\u04F2|\u04F4|\u04F6|\u04F8|\u04FA|\u04FC|\u04FE|\u0500|\u0502|\u0504|\u0506|\u0508|\u050A|\u050C|\u050E|\u0510|\u0512|\u0514|\u0516|\u0518|\u051A|\u051C|\u051E|\u0520|\u0522|\u0531|\u0532|\u0533|\u0534|\u0535|\u0536|\u0537|\u0538|\u0539|\u053A|\u053B|\u053C|\u053D|\u053E|\u053F|\u0540|\u0541|\u0542|\u0543|\u0544|\u0545|\u0546|\u0547|\u0548|\u0549|\u054A|\u054B|\u054C|\u054D|\u054E|\u054F|\u0550|\u0551|\u0552|\u0553|\u0554|\u0555|\u0556|\u10A0|\u10A1|\u10A2|\u10A3|\u10A4|\u10A5|\u10A6|\u10A7|\u10A8|\u10A9|\u10AA|\u10AB|\u10AC|\u10AD|\u10AE|\u10AF|\u10B0|\u10B1|\u10B2|\u10B3|\u10B4|\u10B5|\u10B6|\u10B7|\u10B8|\u10B9|\u10BA|\u10BB|\u10BC|\u10BD|\u10BE|\u10BF|\u10C0|\u10C1|\u10C2|\u10C3|\u10C4|\u10C5|\u1E00|\u1E02|\u1E04|\u1E06|\u1E08|\u1E0A|\u1E0C|\u1E0E|\u1E10|\u1E12|\u1E14|\u1E16|\u1E18|\u1E1A|\u1E1C|\u1E1E|\u1E20|\u1E22|\u1E24|\u1E26|\u1E28|\u1E2A|\u1E2C|\u1E2E|\u1E30|\u1E32|\u1E34|\u1E36|\u1E38|\u1E3A|\u1E3C|\u1E3E|\u1E40|\u1E42|\u1E44|\u1E46|\u1E48|\u1E4A|\u1E4C|\u1E4E|\u1E50|\u1E52|\u1E54|\u1E56|\u1E58|\u1E5A|\u1E5C|\u1E5E|\u1E60|\u1E62|\u1E64|\u1E66|\u1E68|\u1E6A|\u1E6C|\u1E6E|\u1E70|\u1E72|\u1E74|\u1E76|\u1E78|\u1E7A|\u1E7C|\u1E7E|\u1E80|\u1E82|\u1E84|\u1E86|\u1E88|\u1E8A|\u1E8C|\u1E8E|\u1E90|\u1E92|\u1E94|\u1E9E|\u1EA0|\u1EA2|\u1EA4|\u1EA6|\u1EA8|\u1EAA|\u1EAC|\u1EAE|\u1EB0|\u1EB2|\u1EB4|\u1EB6|\u1EB8|\u1EBA|\u1EBC|\u1EBE|\u1EC0|\u1EC2|\u1EC4|\u1EC6|\u1EC8|\u1ECA|\u1ECC|\u1ECE|\u1ED0|\u1ED2|\u1ED4|\u1ED6|\u1ED8|\u1EDA|\u1EDC|\u1EDE|\u1EE0|\u1EE2|\u1EE4|\u1EE6|\u1EE8|\u1EEA|\u1EEC|\u1EEE|\u1EF0|\u1EF2|\u1EF4|\u1EF6|\u1EF8|\u1EFA|\u1EFC|\u1EFE|\u1F08|\u1F09|\u1F0A|\u1F0B|\u1F0C|\u1F0D|\u1F0E|\u1F0F|\u1F18|\u1F19|\u1F1A|\u1F1B|\u1F1C|\u1F1D|\u1F28|\u1F29|\u1F2A|\u1F2B|\u1F2C|\u1F2D|\u1F2E|\u1F2F|\u1F38|\u1F39|\u1F3A|\u1F3B|\u1F3C|\u1F3D|\u1F3E|\u1F3F|\u1F48|\u1F49|\u1F4A|\u1F4B|\u1F4C|\u1F4D|\u1F59|\u1F5B|\u1F5D|\u1F5F|\u1F68|\u1F69|\u1F6A|\u1F6B|\u1F6C|\u1F6D|\u1F6E|\u1F6F|\u1FB8|\u1FB9|\u1FBA|\u1FBB|\u1FC8|\u1FC9|\u1FCA|\u1FCB|\u1FD8|\u1FD9|\u1FDA|\u1FDB|\u1FE8|\u1FE9|\u1FEA|\u1FEB|\u1FEC|\u1FF8|\u1FF9|\u1FFA|\u1FFB|\u2102|\u2107|\u210B|\u210C|\u210D|\u2110|\u2111|\u2112|\u2115|\u2119|\u211A|\u211B|\u211C|\u211D|\u2124|\u2126|\u2128|\u212A|\u212B|\u212C|\u212D|\u2130|\u2131|\u2132|\u2133|\u213E|\u213F|\u2145|\u2183|\u2C00|\u2C01|\u2C02|\u2C03|\u2C04|\u2C05|\u2C06|\u2C07|\u2C08|\u2C09|\u2C0A|\u2C0B|\u2C0C|\u2C0D|\u2C0E|\u2C0F|\u2C10|\u2C11|\u2C12|\u2C13|\u2C14|\u2C15|\u2C16|\u2C17|\u2C18|\u2C19|\u2C1A|\u2C1B|\u2C1C|\u2C1D|\u2C1E|\u2C1F|\u2C20|\u2C21|\u2C22|\u2C23|\u2C24|\u2C25|\u2C26|\u2C27|\u2C28|\u2C29|\u2C2A|\u2C2B|\u2C2C|\u2C2D|\u2C2E|\u2C60|\u2C62|\u2C63|\u2C64|\u2C67|\u2C69|\u2C6B|\u2C6D|\u2C6E|\u2C6F|\u2C72|\u2C75|\u2C80|\u2C82|\u2C84|\u2C86|\u2C88|\u2C8A|\u2C8C|\u2C8E|\u2C90|\u2C92|\u2C94|\u2C96|\u2C98|\u2C9A|\u2C9C|\u2C9E|\u2CA0|\u2CA2|\u2CA4|\u2CA6|\u2CA8|\u2CAA|\u2CAC|\u2CAE|\u2CB0|\u2CB2|\u2CB4|\u2CB6|\u2CB8|\u2CBA|\u2CBC|\u2CBE|\u2CC0|\u2CC2|\u2CC4|\u2CC6|\u2CC8|\u2CCA|\u2CCC|\u2CCE|\u2CD0|\u2CD2|\u2CD4|\u2CD6|\u2CD8|\u2CDA|\u2CDC|\u2CDE|\u2CE0|\u2CE2|\uA640|\uA642|\uA644|\uA646|\uA648|\uA64A|\uA64C|\uA64E|\uA650|\uA652|\uA654|\uA656|\uA658|\uA65A|\uA65C|\uA65E|\uA662|\uA664|\uA666|\uA668|\uA66A|\uA66C|\uA680|\uA682|\uA684|\uA686|\uA688|\uA68A|\uA68C|\uA68E|\uA690|\uA692|\uA694|\uA696|\uA722|\uA724|\uA726|\uA728|\uA72A|\uA72C|\uA72E|\uA732|\uA734|\uA736|\uA738|\uA73A|\uA73C|\uA73E|\uA740|\uA742|\uA744|\uA746|\uA748|\uA74A|\uA74C|\uA74E|\uA750|\uA752|\uA754|\uA756|\uA758|\uA75A|\uA75C|\uA75E|\uA760|\uA762|\uA764|\uA766|\uA768|\uA76A|\uA76C|\uA76E|\uA779|\uA77B|\uA77D|\uA77E|\uA780|\uA782|\uA784|\uA786|\uA78B|\uFF21|\uFF22|\uFF23|\uFF24|\uFF25|\uFF26|\uFF27|\uFF28|\uFF29|\uFF2A|\uFF2B|\uFF2C|\uFF2D|\uFF2E|\uFF2F|\uFF30|\uFF31|\uFF32|\uFF33|\uFF34|\uFF35|\uFF36|\uFF37|\uFF38|\uFF39|\uFF3A)" ". "))

(defn tokenize
  "Split s into tokens delimited by whitespace."
  [s]
  (split s #"\s+|-"))

(defn trim-player-name
  "Trim player's name if it's too long.
  Returns a vector [trimmed-name font-class]."
  [player-name & {:keys [max-length]
                  :or {max-length 20}}]
  (let [player-name-length (count player-name)
        font-class (cond (< player-name-length 6) "font-large"
                         (< player-name-length 10) "font-regular"
                         :else "font-small")
        trimmed-name (if (> player-name-length max-length)
                       (str (subs player-name 0 (- max-length 3)) "...")
                       player-name)]
    [trimmed-name font-class]))

(defn truncate-description
  "Truncate description to the configured maximum length.
  Cuts the description on a complete sentence."
  [description]
  (let [maximum-length (:max-question-length config)]
    (cond (> (count description) maximum-length)
          (reduce (fn [a b]
                    (if (> (count a) maximum-length)
                        a
                        (str a ". " b)))
                  (split description #"\.\s+"))
          :else description)))

(def clear-label
  (comp trim delete-parenthesized-parts))

(def abbreviate
  (comp abbreviate-tokens clear-tokens tokenize clear-label))

(def clear-description
  (comp space-sentences collapse-whitespace))

(def normalize-answer
  "Normalize answer to enable non-exact matching."
  (comp trim
        lower-case
        remove-punctuation
        replace-diacritics))

(defn despoilerify
  "Replace spoilers suggesting label from description"
  [{:keys [label description surfaceForms] :as item}]
  (let [clean-label (clear-label label)
        tokens (clear-tokens (tokenize clean-label))
        abbreviation (abbreviate-tokens tokens)
        ; Sort surface forms from the longest to the shortest, so that we first replace
        ; the longer matches. 
        surface-forms (sort-by (comp - count)
                               (conj (split surfaceForms "|") clean-label label))]
    (assoc item
           :abbreviation abbreviation
           :description (-> description
                            delete-parenthesized-parts
                            clear-description
                            (replace-surface-forms abbreviation surface-forms)
                            truncate-description)
           :label (join " " tokens))))

(defn generate-hint
  [answer & {:keys [percent-revealed]
             :or {percent-revealed 0.2}}]
  (letfn [(reveal [ch] (if (< (rand) percent-revealed) ch "⏑"))]
    (join " "
      (map (comp (fn [[head & tail]] (apply str (cons head (map reveal tail)))) trim)
           (tokenize answer)))))
