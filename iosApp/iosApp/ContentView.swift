import SwiftUI
import ziplinePlayground

struct ContentView: View {
	@State var greet: String = "loading..."

	var body: some View {
		Text(greet).task {
            let service = try! await MyZiplineServiceProvider.shared.obtain()
            do {
                greet = try await service.echo(input: "hello")
            } catch {
                greet = error.localizedDescription
            }
		}
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
