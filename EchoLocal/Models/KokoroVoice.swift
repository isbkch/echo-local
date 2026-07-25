import Foundation

struct KokoroVoice: Identifiable, Hashable {
    let id: String
    let name: String
    let character: String
    let region: String

    var fileName: String {
        "\(id).safetensors"
    }

    var isAmericanEnglish: Bool {
        id.hasPrefix("a")
    }

    static let curated: [KokoroVoice] = [
        KokoroVoice(id: "af_heart", name: "Heart", character: "Balanced and intimate", region: "American"),
        KokoroVoice(id: "af_bella", name: "Bella", character: "Warm and expressive", region: "American"),
        KokoroVoice(id: "am_michael", name: "Michael", character: "Clear and grounded", region: "American"),
        KokoroVoice(id: "bf_emma", name: "Emma", character: "Polished and composed", region: "British"),
        KokoroVoice(id: "bm_george", name: "George", character: "Measured and articulate", region: "British"),
    ]

    static func voice(withID id: String) -> KokoroVoice {
        curated.first(where: { $0.id == id }) ?? curated[0]
    }
}

