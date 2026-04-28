import SwiftUI

/// Renders a vector icon from the Asset Catalog under the `Icons/` namespace.
/// Asset Catalog imagesets are auto-generated from /icons/*.svg by
/// scripts/svg_to_imageset.py (Gradle task `syncIconImageSets`).
struct IconView: View {
    let assetName: String
    var size: CGFloat = 24
    var tint: Color = .primary

    var body: some View {
        Image("Icons/\(assetName)")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundColor(tint)
    }
}
