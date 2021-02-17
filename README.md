# Translator

Uses Google Translate controlled by Selenium for Firefox to auto translate english words.
After some customization, it can be used to auto translate any language pair using also other browsers.

Reads an Excel (xslt) file with a single column, containing english words you want to translate.
For each word, it puts it into the source text area and reads the result.

The application also reads all word meanings grouped by word classes and tryies to extract 5 examples of usage (this not always works).

Ultimately, it writes gathered data into new Excel file.
