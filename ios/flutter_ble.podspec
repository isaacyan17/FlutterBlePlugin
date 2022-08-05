#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_ble.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_ble'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter project.'
  s.description      = <<-DESC
A new Flutter project.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*','gen/**/*'
  s.public_header_files = 'Classes/**/*.h', 'gen/**/*.h'
  s.dependency 'Flutter'
  s.platform = :ios, '9.0'
  s.framework = 'CoreBluetooth'
  s.subspec "Protos" do |ss|
    ss.source_files = "gen/*.pbobjc.{h,m}", "gen/**/*.pbobjc.{h,m}"
    ss.header_mappings_dir = "gen"
    ss.requires_arc = false
    ss.dependency "Protobuf", '~> 3.21.2'
  end

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
