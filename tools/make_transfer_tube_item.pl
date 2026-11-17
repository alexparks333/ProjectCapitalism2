# Builds the Transfer Tube item model (models/item/transfer_tube.json): the whole 3x2 tube out of its block part models,
# shrunk to fit a slot. Run from src/main/resources/assets/capitalism/models after changing a part model.
use strict; use JSON::PP;
my $j = JSON::PP->new->canonical->pretty;
sub load { local $/; open my $f, '<', "block/$_[0].json" or die; decode_json(<$f>) }
# part model, x offset, y offset (tube facing north: left booth is +x, right booth is -x)
my @parts = (['transfer_tube_booth_bottom', 16, 0], ['transfer_tube_booth_top', 16, 16],
             ['transfer_tube_booth_bottom', -16, 0], ['transfer_tube_booth_top', -16, 16],
             ['transfer_tube_console', 0, 0], ['transfer_tube_sign_right', 0, 16]);
my (%tex, @els);
for my $p (@parts) {
  my $m = load($p->[0]);
  %tex = (%tex, %{$m->{textures}});
  for my $e (@{$m->{elements}}) {
    my %c = %$e;
    $c{from} = [$e->{from}[0] + $p->[1], $e->{from}[1] + $p->[2], $e->{from}[2]];
    $c{to}   = [$e->{to}[0] + $p->[1],   $e->{to}[1] + $p->[2],   $e->{to}[2]];
    push @els, \%c;
  }
}
my $model = { parent => 'minecraft:block/block', gui_light => 'side', textures => \%tex, elements => \@els,
  display => {
    gui => { rotation => [20, 200, 0], translation => [0, -2.2, 0], scale => [0.3, 0.3, 0.3] },
    fixed => { rotation => [0, 180, 0], translation => [0, -2.2, 0], scale => [0.3, 0.3, 0.3] },
    ground => { rotation => [0, 0, 0], translation => [0, 2, 0], scale => [0.18, 0.18, 0.18] },
    thirdperson_righthand => { rotation => [75, 225, 0], translation => [0, 2.5, 0], scale => [0.2, 0.2, 0.2] },
    thirdperson_lefthand => { rotation => [75, 225, 0], translation => [0, 2.5, 0], scale => [0.2, 0.2, 0.2] },
    firstperson_righthand => { rotation => [0, 225, 0], translation => [0, 0, 0], scale => [0.2, 0.2, 0.2] },
    firstperson_lefthand => { rotation => [0, 225, 0], translation => [0, 0, 0], scale => [0.2, 0.2, 0.2] } } };
open my $o, '>', 'item/transfer_tube.json'; print $o $j->encode($model);
