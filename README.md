# File encryptor 
___
## Description
This is some basic file encryptor/decryptor. The main principles here are the same as in any similar tool.
Aside from any UI and any other notes the basic principles of how processing works for singular file is as following
* File gets delivered to the encryptor (the main utils class used for encryption is using it as InputStream from whatever user provides)
* First user input password gets used in conjunction to derive a key based off of it and some randomly generated salt ( the cipher then gets based off that Key).
* Then the file header gets recorded and consists of following: 
  * First integer recorded is the version of the app (currently sitting at 1)
  * Next one is the salt length, then followed up by salt itself
  * Next bits of header are the iv length and iv itself (basically the  reason why we can decode file with values of randomly generated salt ignoring any rebuilding from scratch process, by passing it as ``GCMParameterSpec``)
  * Last parts contain base file extension byte size and extension itself ( there were few concerns on how to properly introduce ability to keep users file initial extension and after adding some name formatting for encrypted files it was one of the most reasonable paths)
  * So the total structure of the header in the end is ``[4-byte version][4-byte salt length][salt][4-byte IV length][IV][4-byte extension length][extension UTF-8][ciphertext]``.
* After the header was processed (parsed or collected) using the ``Cipher.update(byte[] input, int inputOffset, int inputLen)`` we proces the file in chunks (encoding or decoding) with an ending work with ``OutputStream`` using doFinal() method of cipher (ensuring the file wasnt tampered with after encoding as it would reasonably throw exception to indicate that)

___
## Usage
The app has GUI so just download repository and run it with JDK, or if you have previously compiled it, you can just run jar as is with any java runtime of version 11+ ( no external dependencies). The GUI should have context notes to help user navigate and should be very intuitive. 
To use, select the files after clicking appropriate button, then do same for output folder where you want to place files and mode you want to run with (decrypt/enmcrypt), then just hit start.
* The files selected will be filtered depending on the mode (if decryption selected, before the process begins all files that do not belong by extension will be filtered out, e.g. on decryption files with .encrypted extension will be only ones left, however you can double /triple and so on encrypt files and i left it intentionally, aside from producing noise files it doesnt do anything else and user is not generally expected to repeatedly do that as file selection drops after processing and is not bound to folder)
* There is file formatting name for the files that is used. Contextual help is given but generally speaking there is few ways to make files more distinguishable for user: Files can have flags such as ``{ext} {name} {date}`` that can be used in junction with regular symbols to make recognizable file batches, as example if you use ``sample.txt`` encryption with name format as the ``{name}_{ext}`` the output will be as ``sample_txt.encrypted``
* There is a lot of error handling and handholding for users i tried to add in case there will be some mismatches or errors will happen mid-encryption ensuring there wont be any leaks. But i cannot guarantee that all of them were foreseen and are accounted for.
___
## Limitations
Other than mentioned requirement of java 11+ only other major restrictions is single file-by-file processing due to usage of heavy performance-wise AES algorithms (i haven't done much testing with heavy size files of 20gb+ and decided not to risk it and use single file processing instead of using executors and calling multiple completables)


___
### Other notes
Hope someone finds this app somewhat useful in their studies. In any case, for whoever is reading, feel free to use any part of this project code if it helps