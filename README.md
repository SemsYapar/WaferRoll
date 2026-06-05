# WaferRoll
https://craftinginterpreters.com/ based language I developed to educate myself

Java temelli ama ilerde C ile tekrar yazılacak. Yerleşik Obfuscate desteği olan bir dil.

Ana kaynağın üstüne eklenen kısımlar:
Obfuscator katmanı
Continue ve Break desteği
str, read, write, open, close, print, println, getline built-in fonksiyonları
visual studio code eklentisi oluşturuldu: https://marketplace.visualstudio.com/items?itemName=sems.waferroll

6/5/26:
- round built-int func eklendi.
- file built-in func larda RandomAccessFile objesine geçildi.
- obfuscateNum func undan virgüllü sayılar çıkarıldı. Bazı yanlış hesaplamalara sebep oluyorlardı
- system.out çıktılarında stringify kullanmama sorunu çözüldü
- vscode text editör eklentisi artık .wro ları da destekliyor
