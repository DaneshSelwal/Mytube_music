import urllib.request
import ssl

ssl._create_default_https_context = ssl._create_unverified_context

opener = urllib.request.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
urllib.request.install_opener(opener)

try:
    urllib.request.urlretrieve("https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Regular.ttf", "app/src/main/res/font/inter_regular.ttf")
    urllib.request.urlretrieve("https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-SemiBold.ttf", "app/src/main/res/font/inter_semibold.ttf")
    urllib.request.urlretrieve("https://github.com/google/fonts/raw/main/ofl/jetbrainsmono/static/JetBrainsMono-Regular.ttf", "app/src/main/res/font/jetbrains_mono_regular.ttf")
    print("Fonts downloaded successfully.")
except Exception as e:
    print(f"Error downloading fonts: {e}")
